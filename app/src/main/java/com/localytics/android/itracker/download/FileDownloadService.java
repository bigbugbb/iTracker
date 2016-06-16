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
public class FileDownloadService extends Service {
    private static final String TAG = makeLogTag(FileDownloadService.class);

    private ThreadPoolExecutor mExecutor;
    private PriorityBlockingQueue<Runnable> mQueue;

    private long KEEP_ALIVE_TIME = 60;

    public void onCreate() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        mQueue = new PriorityBlockingQueue<>(availableProcessors, new DownloadThreadComparator());
        mExecutor = new ThreadPoolExecutor(availableProcessors, availableProcessors,
                KEEP_ALIVE_TIME, TimeUnit.SECONDS, (PriorityBlockingQueue) mQueue);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
    }

    private class FileDownloadThread extends Thread {

    }

    private class DownloadThreadComparator implements Comparator<Runnable> {
        @Override
        public int compare(Runnable lhs, Runnable rhs) {
            return 0;
        }
    }
}
