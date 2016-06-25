package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.MediaDownload;
import com.localytics.android.itracker.data.model.Video;
import com.localytics.android.itracker.download.FileDownloadManager;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;
import com.localytics.android.itracker.provider.TrackerContract.DownloadStatus;
import com.localytics.android.itracker.util.ExternalStorageUtils;
import com.localytics.android.itracker.util.YouTubeExtractor;

import org.joda.time.DateTime;

import java.util.ArrayList;

import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class MediaDownloadActivity extends BaseActivity {
    private final static String TAG = makeLogTag(MediaDownloadActivity.class);

    public final static String EXTRA_VIDEOS_TO_DOWNLOAD = "extra_videos_to_download";

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
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final Context context = getApplicationContext();
        final ArrayList<Video> videos = getIntent().getParcelableArrayListExtra(EXTRA_VIDEOS_TO_DOWNLOAD);
        if (videos != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<ContentProviderOperation> ops = new ArrayList<>(videos.size());
                    for (final Video video : videos) {
                        String targetUrl = "";
                        YouTubeExtractor.Result result = new YouTubeExtractor(video.identifier).extract(null);
                        if (result != null) {
                            Uri videoUri = result.getBestAvaiableQualityVideoUri();
                            if (videoUri != null) {
                                targetUrl = videoUri.toString();
                            }
                        }
                        ops.add(ContentProviderOperation
                                .newInsert(FileDownloads.CONTENT_URI)
                                .withValue(FileDownloads.FILE_ID, video.identifier)
                                .withValue(FileDownloads.TARGET_URL, targetUrl)
                                .withValue(FileDownloads.STATUS, DownloadStatus.INITIALIZED.value())
                                .withValue(FileDownloads.START_TIME, DateTime.now().toString())
                                .build());
                    }
                    try {
                        context.getContentResolver().applyBatch(TrackerContract.CONTENT_AUTHORITY, ops);
                        startDownloadTasks();
                    } catch (RemoteException | OperationApplicationException e) {
                        LOGE(TAG, "Fail to initialize new downloads: " + e);
                    }
                }
            }).start();
        }
    }

    private void startDownloadTasks() {
        Context context = getApplicationContext();
        String availableStatus = String.format("%s,%s", DownloadStatus.INITIALIZED.value(), DownloadStatus.PAUSED.value());
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
                    fdm.setFileDownloadListener(new FileDownloadManager.SimpleFileDownloadListener() {

                    });
                    Uri srcUri = Uri.parse(download.target_url);
                    Uri destUri = Uri.parse(ExternalStorageUtils.getSdCardPath() + "/iTracker/downloads/" + download.identifier);
                    fdm.startDownload(download.identifier, srcUri, destUri);
                }
            } catch (Exception e) {
                LOGE(TAG, "Got error when start triggering the download", e);
            } finally {
                cursor.close();
            }
        }
    }
}
