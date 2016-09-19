package com.itracker.android.service.download;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.itracker.android.Application;
import com.itracker.android.provider.TrackerContract.DownloadStatus;
import com.itracker.android.provider.TrackerContract.FileDownloads;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.itracker.android.utils.LogUtils.LOGD;
import static com.itracker.android.utils.LogUtils.makeLogTag;


public class FileDownloadService extends Service implements FileDownloadTask.OnTaskEndedListener {
    private static final String TAG = makeLogTag(FileDownloadService.class);

    public final static String ACTION_DOWNLOAD_FILE = "com.itracker.android.intent.action.DOWNLOAD_FILE";
    public final static String ACTION_RECOVER_STATUS = "com.itracker.android.intent.action.RECOVER_STATUS";

    private ThreadPoolExecutor mExecutor;
    private PriorityBlockingQueue<Runnable> mQueue;

    private Map<String, FileDownloadTask> mTasks;

    final static String FILE_DOWNLOAD_REQUEST = "file_download_request";

    private int CORE_POOL_SIZE = 2;
    private long KEEP_ALIVE_TIME = 150;

    private static FileDownloadService sInstance;

    public static FileDownloadService getInstance() {
        return sInstance;
    }

    public static Intent createRecoverStatusIntent(Context context) {
        Intent intent = new Intent(context, FileDownloadService.class);
        intent.setAction(FileDownloadService.ACTION_RECOVER_STATUS);
        return intent;
    }

    public static Intent createFileDownloadIntent(Context context, FileDownloadRequest request) {
        Intent intent = new Intent(FileDownloadService.ACTION_DOWNLOAD_FILE);
        intent.setPackage(context.getPackageName());
        intent.putExtra(FileDownloadService.FILE_DOWNLOAD_REQUEST, request);
        return intent;
    }

    public void onCreate() {
        sInstance = this;
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        mQueue = new PriorityBlockingQueue<>(availableProcessors, new DownloadTaskComparator());
        mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, Math.max(CORE_POOL_SIZE, availableProcessors),
                KEEP_ALIVE_TIME, TimeUnit.SECONDS, (PriorityBlockingQueue) mQueue);
        mTasks = Collections.synchronizedMap(new HashMap<String, FileDownloadTask>());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mExecutor.shutdown();
        sInstance = null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent != null) {
            // Need this separate thread to update the download status before starting to download,
            // and make sure it's always running before the following download tasks.
            Application.getInstance().runInBackground(new Runnable() {
                @Override
                public void run() {
                    final String action = intent.getAction();
                    if (ACTION_DOWNLOAD_FILE.equals(action)) {
                        FileDownloadRequest request = intent.getParcelableExtra(FILE_DOWNLOAD_REQUEST);
                        if (request != null) {
                            handleRequest(request);
                        }
                    } else if (ACTION_RECOVER_STATUS.equals(action)) {
                        ContentResolver resolver = getApplicationContext().getContentResolver();
                        ContentValues values = new ContentValues();
                        values.put(FileDownloads.STATUS, DownloadStatus.PENDING.value());
                        resolver.update(
                                FileDownloads.CONTENT_URI,
                                values,
                                FileDownloads.STATUS + " = ? OR " + FileDownloads.STATUS + " = ?",
                                new String[]{DownloadStatus.DOWNLOADING.value(), DownloadStatus.CONNECTING.value()});
                    }
                }
            });
        }
        return START_STICKY;
    }

    private void handleRequest(FileDownloadRequest request) {
        synchronized (this) {
            FileDownloadTask currentTask = mTasks.get(request.mId);
            if (request.mAction == FileDownloadRequest.RequestAction.START) {
                if (currentTask == null) {
                    FileDownloadTask task = new FileDownloadTask(request, this);
                    mTasks.put(request.mId, task);
                    mExecutor.execute(task);
                } else {
                    LOGD(TAG, "Download task " + request.mId + " is already running");
                }
            } else if (request.mAction == FileDownloadRequest.RequestAction.PAUSE) {
                if (currentTask != null) {
                    currentTask.pause();
                    if (mQueue.remove(currentTask)) {
                        currentTask.updateStatus(DownloadStatus.PAUSED);
                    }
                } else {
                    LOGD(TAG, "Download task " + request.mId + " doesn't exist");
                }
            } else if (request.mAction == FileDownloadRequest.RequestAction.CANCEL) {
                if (currentTask != null) {
                    currentTask.cancel();
                    if (mQueue.remove(currentTask)) {
                        currentTask.updateStatus(DownloadStatus.CANCELED);
                    }
                }
            }
        }
    }

    @Override
    public void onTaskEnded(FileDownloadTask task) {
        mTasks.put(task.getRequest().getId(), null);
    }
}
