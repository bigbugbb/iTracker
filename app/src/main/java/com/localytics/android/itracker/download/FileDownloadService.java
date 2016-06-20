package com.localytics.android.itracker.download;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * Created by bigbug on 6/15/16.
 */
class FileDownloadService extends Service {
    private static final String TAG = makeLogTag(FileDownloadService.class);

    private ThreadPoolExecutor mExecutor;
    private PriorityBlockingQueue<Runnable> mQueue;

    private FileDownloadManager mDownloadManager;

    private HashMap<String, FileDownloadTask> mTasks;

    final static String FILE_DOWNLOAD_REQUEST = "file_download_request";

    private int CORE_POOL_SIZE = 2;
    private long KEEP_ALIVE_TIME = 150;

    public void onCreate() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        mQueue = new PriorityBlockingQueue<>(availableProcessors, new DownloadTaskComparator());
        mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, Math.max(CORE_POOL_SIZE, availableProcessors),
                KEEP_ALIVE_TIME, TimeUnit.SECONDS, (PriorityBlockingQueue) mQueue);
        mDownloadManager = FileDownloadManager.getInstance(getApplicationContext());
        mTasks = new HashMap<>();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        mExecutor.shutdown();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            FileDownloadRequest request = intent.getParcelableExtra(FILE_DOWNLOAD_REQUEST);
            if (request != null) {
                handleRequest(request);
            }
        }
        return START_STICKY;
    }

    private void handleRequest(FileDownloadRequest request) {
        synchronized (this) {
            FileDownloadTask currentTask = mTasks.get(request.mId);
            if (request.mAction == FileDownloadRequest.RequestAction.START) {
                if (currentTask == null) {
                    FileDownloadTask task = new FileDownloadTask(request);
                    mTasks.put(request.mId, task);
                    mExecutor.execute(task);
                } else {
                    LOGD(TAG, "Download task " + request.mId + " is already running");
                }
            } else if (request.mAction == FileDownloadRequest.RequestAction.PAUSE) {
                if (currentTask != null) {
                    currentTask.pause();
                } else {
                    LOGD(TAG, "Download task " + request.mId + " doesn't exist");
                }
            } else if (request.mAction == FileDownloadRequest.RequestAction.CANCEL) {
                // TODO: delete all local resource and reset the db status of this file item
            }
        }
    }

    private class FileDownloadTask implements Runnable {

        private FileDownloadRequest mRequest;

        private AtomicBoolean mPaused;

        FileDownloadTask(FileDownloadRequest request) {
            mRequest = request;
            mPaused = new AtomicBoolean();
        }

        void pause() {
            mPaused.set(true);
        }

        @Override
        public void run() {
            File destFile = new File(mRequest.mDestUri.getPath());
            if (destFile.exists()) {

            }
        }
    }

    private class DownloadTaskComparator implements Comparator<Runnable> {
        @Override
        public int compare(Runnable lhs, Runnable rhs) {
            return 0;
        }
    }
}
