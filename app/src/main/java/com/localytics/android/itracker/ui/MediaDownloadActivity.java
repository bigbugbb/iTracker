package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.text.method.DateTimeKeyListener;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Video;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.util.AppQueryHandler;
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
                                .newInsert(TrackerContract.FileDownloads.CONTENT_URI)
                                .withValue(TrackerContract.FileDownloads.FILE_ID, video.identifier)
                                .withValue(TrackerContract.FileDownloads.TARGET_URL, targetUrl)
                                .withValue(TrackerContract.FileDownloads.STATUS, TrackerContract.DownloadStatus.INITIALIZED.value())
                                .withValue(TrackerContract.FileDownloads.START_TIME, DateTime.now().toString())
                                .build());
                    }
                    try {
                        context.getContentResolver().applyBatch(TrackerContract.CONTENT_AUTHORITY, ops);
                    } catch (RemoteException | OperationApplicationException e) {
                        LOGE(TAG, "Fail to initialize new downloads: " + e);
                    }
                }
            }).start();
        }
    }
}
