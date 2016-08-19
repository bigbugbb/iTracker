package com.localytics.android.itracker.service.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.receiver.FileDownloadReceiver;
import com.localytics.android.itracker.data.FileDownloadManager;
import com.localytics.android.itracker.provider.TrackerContract.MediaDownloads;
import com.localytics.android.itracker.provider.TrackerContract.DownloadStatus;
import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;
import com.localytics.android.itracker.utils.PrefUtils;
import com.localytics.android.itracker.utils.YouTubeExtractor;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.localytics.android.itracker.utils.LogUtils.LOGD;
import static com.localytics.android.itracker.utils.LogUtils.LOGE;
import static com.localytics.android.itracker.utils.LogUtils.LOGI;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;


public class FileDownloadService extends Service {
    private static final String TAG = makeLogTag(FileDownloadService.class);

    public final static String ACTION_DOWNLOAD_FILE = "com.localytics.android.itracker.intent.action.DOWNLOAD_FILE";
    public final static String ACTION_RECOVER_STATUS = "com.localytics.android.itracker.intent.action.RECOVER_STATUS";

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private ThreadPoolExecutor mExecutor;
    private PriorityBlockingQueue<Runnable> mQueue;

    private Map<String, FileDownloadTask> mTasks;

    private LocalBroadcastManager mBroadcastManager;

    private FileDownloadNotificationBuilder mNotificationBuilder;

    final static String FILE_DOWNLOAD_REQUEST = "file_download_request";

    private int CORE_POOL_SIZE = 2;
    private long KEEP_ALIVE_TIME = 150;
    private int RECONNECT_COUNT = 5;

    private String INTERRUPT_BY_PAUSE = "interrupt_by_pause";
    private String INTERRUPT_BY_CANCEL = "interrupt_by_cancel";

    private static FileDownloadService sInstance;

    public static FileDownloadService getInstance() {
        return sInstance;
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, FileDownloadService.class);
    }

    public static Intent createFileDownloadIntent(Context context, FileDownloadRequest request) {
        Intent intent = new Intent(FileDownloadService.ACTION_DOWNLOAD_FILE);
        intent.setPackage(context.getPackageName());
        intent.putExtra(FileDownloadService.FILE_DOWNLOAD_REQUEST, request);
        return intent;
    }

    public void onCreate() {
        sInstance = this;
        mHandlerThread = new HandlerThread("FileDownload");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        FileDownloadManager.getInstance();

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        mQueue = new PriorityBlockingQueue<>(availableProcessors, new DownloadTaskComparator());
        mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, Math.max(CORE_POOL_SIZE, availableProcessors),
                KEEP_ALIVE_TIME, TimeUnit.SECONDS, (PriorityBlockingQueue) mQueue);
        mTasks = Collections.synchronizedMap(new HashMap<String, FileDownloadTask>());
        mBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        mNotificationBuilder = FileDownloadNotificationBuilder.getInstance();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mExecutor.shutdown();
        mHandlerThread.quitSafely();
        sInstance = null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent != null) {
            // Need this separate thread to update the download status before starting to download,
            // and make sure it's always running before the following download tasks.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleIntent(intent);
                }
            });
        }
        return START_STICKY;
    }

    private void handleIntent(final Intent intent) {
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
                    new String[]{ DownloadStatus.DOWNLOADING.value(), DownloadStatus.CONNECTING.value() });
        }
    }

    private void handleRequest(FileDownloadRequest request) {
        synchronized (this) {
            FileDownloadTask currentTask = mTasks.get(request.mId);
            if (request.mAction == FileDownloadRequest.RequestAction.START) {
                if (currentTask == null) {
                    FileDownloadTask task = new FileDownloadTask(getApplicationContext(), request);
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

    private class FileDownloadTask implements Runnable {

        private Context mContext;
        private ContentResolver mResolver;
        private FileDownloadRequest mRequest;

        private AtomicBoolean mPaused;
        private AtomicBoolean mCanceled;

        private String mContentType;
        private DownloadStatus mStatus;

        private Map<String, String> mDownloadInfo;

        private final static int BUFFER_SIZE = 1024 * 32;

        FileDownloadTask(Context context, FileDownloadRequest request) {
            mContext = context;
            mRequest = request;
            mPaused = new AtomicBoolean();
            mCanceled = new AtomicBoolean();
            mResolver = context.getContentResolver();
            mStatus = DownloadStatus.PENDING;
        }

        void pause() {
            mPaused.set(true);
        }

        void cancel() {
            mCanceled.set(true);
        }

        private void download(int retries) throws Exception {
            boolean reconnect = false;
            InputStream input = null;
            RandomAccessFile output = null;

            try {
                long currentFileSize, totalFileSize = 0, readBetweenInterval = 0;

                // Query for any existing download record for this media
                Cursor cursor = mResolver.query(FileDownloads.CONTENT_URI, null, FileDownloads.FILE_ID + " = ?", new String[]{mRequest.mId}, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            totalFileSize = cursor.getLong(cursor.getColumnIndex(FileDownloads.TOTAL_SIZE));
                        }
                    } finally {
                        cursor.close();
                    }
                }

                // Retrieve existing local file size if exists, otherwise create a new file
                File destFile = new File(mRequest.mDestUri.toString());
                destFile.getParentFile().mkdirs();
                output = new RandomAccessFile(destFile, "rw");
                currentFileSize = output.length();
                output.seek(currentFileSize);

                // Try to connect the download url (with location redirect)
                URL url = new URL(mRequest.mSrcUri.toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("User-Agent", "Mozilla/5.0...");
                connection.setRequestProperty("Range", "bytes=" + currentFileSize + "-");
                LOGI(TAG, "Original URL: " + connection.getURL());
                connection.connect();
                LOGI(TAG, "Connected URL: " + connection.getURL());
                input = connection.getInputStream();
                LOGI(TAG, "Redirected URL: " + connection.getURL());

                /***** Downloading *****/
                updateStatus(DownloadStatus.DOWNLOADING, false);

                ContentValues values = new ContentValues();
                values.put(FileDownloads.STATUS, mStatus.value());
                if (totalFileSize == 0) {
                    totalFileSize = Long.parseLong(connection.getHeaderField("Content-Length"));
                    values.put(FileDownloads.TOTAL_SIZE, totalFileSize);
                }
                mResolver.update(FileDownloads.CONTENT_URI, values, String.format("%s = ?", FileDownloads.FILE_ID), new String[]{mRequest.mId});

                mContentType = connection.getHeaderField("Content-Type");

                // Keep downloading the file
                long bytesToWrite = totalFileSize - currentFileSize;
                byte[] buffer = new byte[BUFFER_SIZE];
                long prevTime = SystemClock.uptimeMillis();
                while (bytesToWrite > 0) {
                    int read;
                    if (bytesToWrite > BUFFER_SIZE) {
                        read = input.read(buffer);
                    } else {
                        read = input.read(buffer, 0, (int) bytesToWrite);
                    }
                    output.write(buffer, 0, read);

                    readBetweenInterval += read;

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

                    currentFileSize += read;
                    bytesToWrite -= read;

                    long timeInterval = SystemClock.uptimeMillis() - prevTime;
                    if (timeInterval > DateUtils.SECOND_IN_MILLIS) {
                        long downloadSpeed = (long) (readBetweenInterval / (timeInterval / (float) DateUtils.SECOND_IN_MILLIS));
                        onDownloading(currentFileSize, totalFileSize, downloadSpeed);
                        prevTime = SystemClock.uptimeMillis();
                        readBetweenInterval = 0;
                    }
                }
            } catch (IOException e) {
                if (e instanceof DownloadInterruptedException) {
                    throw e;
                } else {
                    LOGE(TAG, "Something wrong with the connection", e);
                    if (retries >= RECONNECT_COUNT) {
                        throw e;
                    }
                    reconnect = true;
                }
            } finally {
                closeInput(input);
                closeOutput(output);
            }

            if (reconnect && retries < RECONNECT_COUNT) {
                updateStatus(DownloadStatus.CONNECTING);
                waitBeforeReconnect(retries);
                download(retries + 1);
            }
        }

        private void waitBeforeReconnect(int retries) {
            try {
                long time = DateUtils.SECOND_IN_MILLIS * (long) Math.pow(2, retries);
                Thread.sleep(time);
            } catch (InterruptedException e) {
                // Nothing to do
            }
        }

        @Override
        public void run() {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                /***** Preparing *****/
                updateStatus(DownloadStatus.PREPARING);

                onPreparing();

                updateUri();

                download(0);

                /***** Completed *****/
                File finalFile;
                if ((finalFile = updateFileName(mDownloadInfo.get(MediaDownloads.TITLE))) == null) {
                    throw new IOException("Can't generate the final file");
                } else {
                    LOGD(TAG, String.format("Download %s has been completed (file: %s)", mRequest.mId, finalFile));
                    updateLocalLocation(finalFile);
                    updateStatus(DownloadStatus.COMPLETED);
                    onCompleted(Uri.fromFile(finalFile));
                }
            } catch (DownloadInterruptedException e) {
                if (e.getReason().equals(INTERRUPT_BY_PAUSE)) {
                    LOGD(TAG, String.format("Download %s has been paused", mRequest.mId), e);
                    updateStatus(DownloadStatus.PAUSED);
                    onPaused();
                } else if (e.getReason().equals(INTERRUPT_BY_CANCEL)) {
                    LOGD(TAG, String.format("Download %s has been canceled", mRequest.mId), e);
                    updateStatus(DownloadStatus.CANCELED);
                    onCanceled();
                }
            } catch (Exception e) {
                LOGE(TAG, String.format("Failed to download %s from %s", mRequest.mId, mRequest.mSrcUri), e);
                updateStatus(DownloadStatus.FAILED);
                onFailed(e);
            } finally {
                if (mCanceled.get()) {
                    File destFile = new File(mRequest.mDestUri.toString());
                    destFile.delete();
                }

                mTasks.put(mRequest.mId, null);
            }
        }

        private void updateUri() throws Exception {
            if (mRequest.mSrcUri == null) {
                YouTubeExtractor.Result result = new YouTubeExtractor(mRequest.getId()).extract(null);
                if (result != null) {
                    mRequest.mSrcUri = result.getBestAvaiableQualityVideoUri();
                    if (mRequest.mSrcUri == null) {
                        throw new Exception("Can't get the uri for video " + mRequest.getId());
                    }
                }
            }
            if (mRequest.mDestUri == null) {
                mRequest.mDestUri = Uri.parse(Config.FILE_DOWNLOAD_DIR_PATH + mRequest.getId());
            }
        }

        private File updateFileName(String fileTitle) {
            String type = mContentType.split("[/]")[1]; // Assume the content type is always correct from youtube
            String path = mRequest.mDestUri.getPath();
            path = path.substring(0, path.lastIndexOf("/") + 1) + fileTitle.replace(" ", "_");
            File oldFile = new File(mRequest.mDestUri.toString());
            File newFile = new File(path + "." + type);
            for (int i = 1; newFile.exists(); ++i) {
                newFile = new File(String.format("%s(%d).%s", path, i, type));
            }
            if (oldFile.renameTo(newFile)) {
                return newFile;
            }
            return null;
        }

        private Map<String, String> getDownloadInfo() {
            Map<String, String> downloadInfo = new HashMap<>();

            String title = null, startTime = null;
            Cursor cursor = mContext.getContentResolver().query(
                    FileDownloads.MEDIA_DOWNLOADS_URI,
                    null,
                    FileDownloads.FILE_ID + " = ?",
                    new String[] { mRequest.mId },
                    null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        title = cursor.getString(cursor.getColumnIndex(MediaDownloads.TITLE));
                        startTime = cursor.getString(cursor.getColumnIndex(MediaDownloads.START_TIME));
                    }
                } finally {
                    cursor.close();
                }
            }

            downloadInfo.put(MediaDownloads.TITLE, TextUtils.isEmpty(title) ? mRequest.getId() : title);

            DateTimeFormatter formatter = DateTimeFormat.forPattern("hh:mm a");
            if (TextUtils.isEmpty(startTime)) {
                startTime = DateTime.now().toString(formatter);
            } else {
                startTime = ISODateTimeFormat.dateTime().parseDateTime(startTime).toString(formatter);
            }
            startTime = startTime.startsWith("0") ? startTime.substring(1) : startTime;
            downloadInfo.put(MediaDownloads.START_TIME, startTime);

            return downloadInfo;
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

        private void closeOutput(RandomAccessFile output) {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void onPreparing() {
            Intent intent = new Intent(FileDownloadReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);
            intent.putExtra(FileDownloadReceiver.CURRENT_DOWNLOAD_STAGE, FileDownloadReceiver.DOWNLOAD_STAGE_PREPARING);
            intent.putExtra(FileDownloadReceiver.CURRENT_REQUEST, mRequest);
            mBroadcastManager.sendBroadcast(intent);
            mDownloadInfo = getDownloadInfo();
        }

        protected void onPaused() {
            Intent intent = new Intent(FileDownloadReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);
            intent.putExtra(FileDownloadReceiver.CURRENT_DOWNLOAD_STAGE, FileDownloadReceiver.DOWNLOAD_STAGE_PAUSED);
            intent.putExtra(FileDownloadReceiver.CURRENT_REQUEST, mRequest);
            mBroadcastManager.sendBroadcast(intent);

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(mRequest.getId().hashCode());
        }

        protected void onDownloading(long currentFileSize, long totalFileSize, long downloadSpeed) {
            // Store the data so we can use them to update ui even if the download is stopped.
            PrefUtils.setCurrentDownloadSpeed(mContext, mRequest.getId(), downloadSpeed);
            PrefUtils.setCurrentDownloadFileSize(mContext, mRequest.getId(), currentFileSize);

            Intent intent = new Intent(FileDownloadReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);
            intent.putExtra(FileDownloadReceiver.CURRENT_DOWNLOAD_STAGE, FileDownloadReceiver.DOWNLOAD_STAGE_DOWNLOADING);
            intent.putExtra(FileDownloadReceiver.CURRENT_REQUEST, mRequest);
            intent.putExtra(FileDownloadReceiver.CURRENT_FILE_SIZE_BYTES, currentFileSize);
            intent.putExtra(FileDownloadReceiver.TOTAL_FILE_SIZE_BYTES, totalFileSize);
            intent.putExtra(FileDownloadReceiver.DOWNLOAD_SPEED, downloadSpeed);
            mBroadcastManager.sendBroadcast(intent);

            int progress = (int) (100 * (float) currentFileSize / totalFileSize);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = mNotificationBuilder.newDownloadingNotification(mRequest.getId(), progress, mDownloadInfo);
            nm.notify(mRequest.getId().hashCode(), notification);
        }

        protected void onCanceled() {
            File tempFile = new File(mRequest.mDestUri.toString());
            tempFile.delete();
            Intent intent = new Intent(FileDownloadReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);
            intent.putExtra(FileDownloadReceiver.CURRENT_DOWNLOAD_STAGE, FileDownloadReceiver.DOWNLOAD_STAGE_CANCELED);
            intent.putExtra(FileDownloadReceiver.CURRENT_REQUEST, mRequest);
            mBroadcastManager.sendBroadcast(intent);

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(mRequest.getId().hashCode());
        }

        protected void onCompleted(Uri finalFileUri) {
            Intent intent = new Intent(FileDownloadReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);
            intent.putExtra(FileDownloadReceiver.CURRENT_DOWNLOAD_STAGE, FileDownloadReceiver.DOWNLOAD_STAGE_COMPLETED);
            intent.putExtra(FileDownloadReceiver.CURRENT_REQUEST, mRequest);
            intent.putExtra(FileDownloadReceiver.DOWNLOADED_FILE_URI, finalFileUri);
            mBroadcastManager.sendBroadcast(intent);

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = mNotificationBuilder.newCompletedNotification(mRequest.getId(), finalFileUri, mDownloadInfo);
            nm.notify(mRequest.getId().hashCode(), notification);
        }

        protected void onFailed(Exception exception) {
            String reason = exception.getMessage();
            reason = TextUtils.isEmpty(reason) ? exception.toString() : reason;

            Intent intent = new Intent(FileDownloadReceiver.ACTION_FILE_DOWNLOAD_PROGRESS);
            intent.putExtra(FileDownloadReceiver.CURRENT_DOWNLOAD_STAGE, FileDownloadReceiver.DOWNLOAD_STAGE_FAILED);
            intent.putExtra(FileDownloadReceiver.CURRENT_REQUEST, mRequest);
            intent.putExtra(FileDownloadReceiver.DOWNLOAD_FAILED_REASON, reason);
            mBroadcastManager.sendBroadcast(intent);

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = mNotificationBuilder.newFailedNotification(mRequest.getId(), mDownloadInfo, reason);
            nm.notify(mRequest.getId().hashCode(), notification);
        }

        void updateLocalLocation(File downloadedFile) {
            ContentValues values = new ContentValues();
            values.put(FileDownloads.LOCAL_LOCATION, downloadedFile.toString());
            mResolver.update(FileDownloads.CONTENT_URI, values, String.format("%s = ?", FileDownloads.FILE_ID), new String[]{mRequest.mId});
        }

        void updateStatus(DownloadStatus status) {
            updateStatus(status, true);
        }

        void updateStatus(DownloadStatus status, boolean syncDb) {
            synchronized (this) {
                mStatus = status;
            }

            if (syncDb) {
                ContentValues values = new ContentValues();
                values.put(FileDownloads.STATUS, mStatus.value());
                mResolver.update(FileDownloads.CONTENT_URI, values, String.format("%s = ?", FileDownloads.FILE_ID), new String[]{ mRequest.mId });
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
