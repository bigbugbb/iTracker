package com.localytics.android.itracker.download;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * Created by bigbug on 6/15/16.
 */
class FileDownloadService extends Service {
    private static final String TAG = makeLogTag(FileDownloadService.class);

    private ThreadPoolExecutor mExecutor;
    private PriorityBlockingQueue<Runnable> mQueue;

    private FileDownloadManager mFileDownloadManager;

    final static String FILE_DOWNLOAD_REQUEST = "file_download_request";

    private int CORE_POOL_SIZE = 2;
    private long KEEP_ALIVE_TIME = 150;

    public void onCreate() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        mQueue = new PriorityBlockingQueue<>(availableProcessors, new DownloadTaskComparator());
        mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, Math.max(CORE_POOL_SIZE, availableProcessors),
                KEEP_ALIVE_TIME, TimeUnit.SECONDS, (PriorityBlockingQueue) mQueue);
        mFileDownloadManager = FileDownloadManager.getInstance(getApplicationContext());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            FileDownloadRequest request = intent.getParcelableExtra(FILE_DOWNLOAD_REQUEST);
            FileDownloadTask task = new FileDownloadTask(request);
            mExecutor.execute(task);
        }
        return START_STICKY;
    }

    private class FileDownloadTask implements Runnable {

        private FileDownloadRequest mRequest;

        FileDownloadTask(FileDownloadRequest request) {
            mRequest = request;
        }

        @Override
        public void run() {
            // TODO:
        }
    }

    private class DownloadTaskComparator implements Comparator<Runnable> {
        @Override
        public int compare(Runnable lhs, Runnable rhs) {
            return 0;
        }
    }
}
