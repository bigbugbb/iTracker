package com.localytics.android.itracker.download;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import com.localytics.android.itracker.provider.TrackerContract.DownloadStatus;
import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;
import com.localytics.android.itracker.util.YouTubeExtractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.LOGI;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * Created by bigbug on 6/15/16.
 */
class FileDownloadService extends Service {
    private static final String TAG = makeLogTag(FileDownloadService.class);

    private ThreadPoolExecutor mExecutor;
    private PriorityBlockingQueue<Runnable> mQueue;

    private Handler mHandler;
    private FileDownloadManager mDownloadManager;

    private HashMap<String, FileDownloadTask> mTasks;

    final static String FILE_DOWNLOAD_REQUEST = "file_download_request";

    private int CORE_POOL_SIZE = 2;
    private long KEEP_ALIVE_TIME = 150;

    private String INTERRUPT_BY_PAUSE = "interrupt_by_pause";
    private String INTERRUPT_BY_CANCEL = "interrupt_by_cancel";

    public void onCreate() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        mQueue = new PriorityBlockingQueue<>(availableProcessors, new DownloadTaskComparator());
        mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, Math.max(CORE_POOL_SIZE, availableProcessors),
                KEEP_ALIVE_TIME, TimeUnit.SECONDS, (PriorityBlockingQueue) mQueue);
        mHandler = new Handler();
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
                    Context context = getApplicationContext();
                    FileDownloadTask task = new FileDownloadTask(context, request);
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

    class FileDownloadTask implements Runnable {

        private Context mContext;
        private ContentResolver mResolver;
        private FileDownloadRequest mRequest;

        private AtomicBoolean mPaused;
        private AtomicBoolean mCanceled;

        private long mCurrentFileSize;
        private long mTotalFileSize;
        private DownloadStatus mStatus;

        private FileDownloadListener mListener;

        FileDownloadTask(Context context, FileDownloadRequest request) {
            mContext = context;
            mRequest = request;
            mPaused = new AtomicBoolean();
            mCanceled = new AtomicBoolean();
            mCurrentFileSize = 0;
            mTotalFileSize = 0;
            mResolver = context.getContentResolver();
            mListener = mDownloadManager.getFileDownloadListener();
            mStatus = DownloadStatus.INITIALIZED;
        }

        void pause() {
            mPaused.set(true);
        }

        @Override
        public void run() {
            InputStream input = null;
            OutputStream output = null;
            try {
                /***** Preparing *****/
                updateStatus(DownloadStatus.PREPARING, true);

                onPrepare();

                // Query for any existing download record for this media
                Uri prevSrcUri = null;
                Cursor cursor = mResolver.query(
                        FileDownloads.CONTENT_URI,
                        null,
                        String.format("%s = ?", FileDownloads.MEDIA_ID),
                        new String[]{ mRequest.mId },
                        null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            updateStatus(DownloadStatus.valueOf(cursor.getString(cursor.getColumnIndex(FileDownloads.STATUS))), false);
                            mTotalFileSize = cursor.getLong(cursor.getColumnIndex(FileDownloads.MEDIA_SIZE));
                            prevSrcUri = Uri.parse(cursor.getString(cursor.getColumnIndex(FileDownloads.TARGET_URL)));
                            if (mRequest.mSrcUri == null) {
                                mRequest.mSrcUri = prevSrcUri;
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }

                // Extract source uri if it's not included in the request
                if (mRequest.mSrcUri == null) {
                    new YouTubeExtractor(mRequest.mId).extract(new YouTubeExtractor.Callback() {
                        @Override
                        public void onSuccess(YouTubeExtractor.Result result) {
                            mRequest.mSrcUri = result.getBestAvaiableQualityVideoUri();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            LOGE(TAG, "Can't get the file url");
                        }
                    });

                    if (mRequest.mSrcUri == null) {
                        LOGE(TAG, "Can't get the target url of the source file");
                        onFailed();
                        return;
                    }
                }

                // Retrieve existing local file size if exists, otherwise create a new file
                File destFile = new File(mRequest.mDestUri.getPath());
                if (!destFile.exists()) {
                    destFile.getParentFile().mkdirs();
                    destFile.createNewFile();
                } else {
                    // Check whether the previous source uri diffs from the current, if so we should
                    // make a total new file, otherwise continue to download the previous fille
                    if (prevSrcUri != null) {
                        mRequest.mSrcUri = prevSrcUri; // still download from the old location
                        mCurrentFileSize = destFile.length();
                    } else {
                        // Invalid condition, just delete the existing file and start a new download
                        destFile.delete();
                        destFile.createNewFile();
                    }
                }

                ContentValues values = new ContentValues();
                values.put(FileDownloads.TARGET_URL, mRequest.mSrcUri.getPath());
                mResolver.update(FileDownloads.CONTENT_URI, values, String.format("%s = ?", FileDownloads.MEDIA_ID), new String[]{ mRequest.mId });

                // Try to connect the download url (with location redirect)
                URL url = new URL(mRequest.mSrcUri.getPath());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0...");
                connection.setRequestProperty("Range", "bytes=" + mCurrentFileSize + "-");
                LOGI(TAG, "Original URL: " + connection.getURL());
                connection.connect();
                LOGI(TAG, "Connected URL: " + connection.getURL());
                input = connection.getInputStream();
                LOGI(TAG, "Redirected URL: " + connection.getURL());

                /***** Downloading *****/
                updateStatus(DownloadStatus.DOWNLOADING, false);

                values = new ContentValues();
                values.put(FileDownloads.STATUS, mStatus.value());
                if (mTotalFileSize == 0) {
                    mTotalFileSize = Long.parseLong(connection.getHeaderField("Content-Length"));
                    values.put(FileDownloads.MEDIA_SIZE, mTotalFileSize);
                }
                mResolver.update(FileDownloads.CONTENT_URI, values, String.format("%s = ?", FileDownloads.MEDIA_ID), new String[]{ mRequest.mId });

                // Keep downloading the file
                output = new FileOutputStream(destFile);
                byte[] buffer = new byte[1024 * 32];
                long prevTime = SystemClock.uptimeMillis();
                for (int read = input.read(buffer); read > 0;) {
                    output.write(buffer, 0, read);

                    // Pause means cancel the download task but retain the partial downloaded file
                    if (mPaused.get()) {
                        throw new DownloadInterruptedException(
                                "Download has been paused but you can call startDownload again to resume it",
                                INTERRUPT_BY_PAUSE);
                    }

                    if (mCanceled.get()) {
                        throw new DownloadInterruptedException(
                                "Download has been canceled and the data downloaded will be removed",
                                INTERRUPT_BY_CANCEL);
                    }

                    mCurrentFileSize += read;

                    if (SystemClock.uptimeMillis() - prevTime > 1000) {
                        onProgress(mCurrentFileSize, mTotalFileSize);
                        prevTime = SystemClock.uptimeMillis();
                    }
                }
                output.flush();

                /***** Completed *****/
                updateStatus(DownloadStatus.COMPLETED, true);

            } catch (DownloadInterruptedException e) {
                if (e.getReason().equals(INTERRUPT_BY_PAUSE)) {
                    LOGD(TAG, String.format("Download %s has been paused", mRequest.mId), e);
                    updateStatus(DownloadStatus.PAUSED, true);
                    onPaused();
                } else if (e.getReason().equals(INTERRUPT_BY_CANCEL)) {
                    LOGD(TAG, String.format("Download %s has been canceled", mRequest.mId), e);
                    updateStatus(DownloadStatus.CANCELED, true);
                    onCanceled();
                }
            } catch (IOException e) {
                LOGE(TAG, String.format("Failed to download %s from %s", mRequest.mId, mRequest.mSrcUri), e);
                updateStatus(DownloadStatus.FAILED, true);
                onFailed();
            } finally {
                closeInput(input);
                closeOutput(output);

                if (mCanceled.get()) {
                    File destFile = new File(mRequest.mDestUri.getPath());
                    destFile.delete();
                }
            }
        }

        private void closeInput(InputStream input) {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void closeOutput(OutputStream output) {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void onPrepare() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.onPrepare(mRequest.mId);
                    }
                }
            });
        }

        protected void onPaused() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.onPaused(mRequest.mId);
                    }
                }
            });
        }

        protected void onProgress(final long currentFileSize, final long totalFileSize) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.onProgress(mRequest.mId, currentFileSize, totalFileSize);
                    }
                }
            });
        }

        protected void onCanceled() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.onCanceled(mRequest.mId);
                    }
                }
            });
        }

        protected void onCompleted() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.onCompleted(mRequest.mId);
                    }
                }
            });
        }

        protected void onFailed() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.onCanceled(mRequest.mId);
                    }
                }
            });
        }

        private void updateStatus(DownloadStatus status, boolean syncDb) {
            synchronized (this) {
                mStatus = status;
            }

            if (syncDb) {
                ContentValues values = new ContentValues();
                values.put(FileDownloads.STATUS, mStatus.value());
                mResolver.update(FileDownloads.CONTENT_URI, values, String.format("%s = ?", FileDownloads.MEDIA_ID), new String[]{ mRequest.mId });
            }
        }
    }

    private class DownloadTaskComparator implements Comparator<Runnable> {
        @Override
        public int compare(Runnable lhs, Runnable rhs) {
            return 0;
        }
    }

    /**
     * Thrown when a download is interrupted.
     */
    public class DownloadInterruptedException extends IOException {

        private String mReason;

        /**
         * Constructs a new {@code FileDownloadInterruptedException} with its stack trace
         * filled in.
         */
        public DownloadInterruptedException() {
        }

        /**
         * Constructs a new {@code FileDownloadInterruptedException} with its stack trace and
         * detail message filled in.
         *
         * @param detailMessage
         *            the detail message for this exception.
         */
        public DownloadInterruptedException(String detailMessage) {
            super(detailMessage);
        }

        /**
         * Constructs a new {@code FileDownloadInterruptedException} with its stack trace and
         * detail message filled in.
         *
         * @param detailMessage
         *            the detail message for this exception.
         * @param reason
         *            the reason for this interruption
         */
        public DownloadInterruptedException(String detailMessage, String reason) {
            super(detailMessage);
            mReason = reason;
        }

        public String getReason() {
            return mReason;
        }
    }
}
