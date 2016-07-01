package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;

import com.localytics.android.itracker.Config;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.MediaDownload;
import com.localytics.android.itracker.data.model.Video;
import com.localytics.android.itracker.download.FileDownloadManager;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.DownloadStatus;
import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;
import com.localytics.android.itracker.util.YouTubeExtractor;

import org.joda.time.DateTime;

import java.util.ArrayList;

import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class MediaDownloadActivity extends BaseActivity {
    private final static String TAG = makeLogTag(MediaDownloadActivity.class);

    public final static String EXTRA_VIDEOS_TO_DOWNLOAD = "extra_videos_to_download";

    private RequestHandler mRequestHandler;
    private HandlerThread  mRequestThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_download);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        Fragment fragment = getFragmentManager().findFragmentById(R.id.media_download_fragment);
        if (fragment == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.media_download_fragment, new MediaDownloadFragment())
                    .commit();
        }

        mRequestThread = new HandlerThread("DownloadRequest");
        mRequestThread.start();

        mRequestHandler = new RequestHandler(mRequestThread.getLooper());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRequestThread.quitSafely();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final ArrayList<Video> videos = getIntent().getParcelableArrayListExtra(EXTRA_VIDEOS_TO_DOWNLOAD);
        if (videos != null && videos.size() > 0) {
            mRequestHandler.obtainMessage(MESSAGE_REQUEST_DOWNLOADS, videos).sendToTarget();
        }
    }

    private void startDownloadTasks() {
        Context context = getApplicationContext();
        String availableStatus = String.format("%s,%s,%s",
                DownloadStatus.PENDING.value(), DownloadStatus.PREPARING.value(), DownloadStatus.DOWNLOADING.value());
        Cursor cursor = context.getContentResolver().query(
                FileDownloads.buildMediaDownloadUriByStatus(availableStatus),
                null,
                null,
                null,
                TrackerContract.FileDownloads.START_TIME + " DESC");
        if (cursor != null) {
            try {
                MediaDownload[] downloads = MediaDownload.downloadsFromCursor(cursor);
                FileDownloadManager fdm = FileDownloadManager.getInstance(context);
                for (MediaDownload download : downloads) {
                    Uri srcUri = Uri.parse(getVideoTargetUrl(download.identifier));
                    Uri destUri = Uri.parse(Config.FILE_DOWNLOAD_DIR_PATH + download.identifier);
                    fdm.startDownload(download.identifier, srcUri, destUri);
                }
            } catch (Exception e) {
                LOGE(TAG, "Got error when start triggering the download", e);
            } finally {
                cursor.close();
            }
        }
    }

    private static final int MESSAGE_REQUEST_DOWNLOADS = 100;

    private class RequestHandler extends Handler {

        public RequestHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Context context = getApplicationContext();
            ArrayList<Video> videos = (ArrayList<Video>) msg.obj;
            try {
                ArrayList<ContentProviderOperation> ops = new ArrayList<>(videos.size());
                for (final Video video : videos) {
                    ops.add(ContentProviderOperation
                            .newInsert(FileDownloads.CONTENT_URI)
                            .withValue(FileDownloads.FILE_ID, video.identifier)
                            .withValue(FileDownloads.STATUS, DownloadStatus.PENDING.value())
                            .withValue(FileDownloads.START_TIME, DateTime.now().toString())
                            .build());
                }
                try {
                    context.getContentResolver().applyBatch(TrackerContract.CONTENT_AUTHORITY, ops);
                    startDownloadTasks();
                } catch (RemoteException | OperationApplicationException e) {
                    LOGE(TAG, "Fail to initialize new downloads: " + e);
                }
            } catch (Exception e) {
                LOGE(TAG, "Fail to make the download request", e);
            }
        }
    }

    private String getVideoTargetUrl(final String videoId) {
        YouTubeExtractor.Result result = new YouTubeExtractor(videoId).extract(null);
        if (result != null) {
            Uri videoUri = result.getBestAvaiableQualityVideoUri();
            if (videoUri != null) {
                return videoUri.toString();
            }
        }
        return "";
    }
}
