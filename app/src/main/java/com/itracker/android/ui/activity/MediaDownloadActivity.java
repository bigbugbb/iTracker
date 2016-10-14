package com.itracker.android.ui.activity;

import android.app.Fragment;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.widget.Toast;

import com.itracker.android.Application;
import com.itracker.android.Config;
import com.itracker.android.R;
import com.itracker.android.data.FileDownloadManager;
import com.itracker.android.data.model.Video;
import com.itracker.android.provider.TrackerContract;
import com.itracker.android.provider.TrackerContract.DownloadStatus;
import com.itracker.android.provider.TrackerContract.FileDownloads;
import com.itracker.android.ui.fragment.MediaDownloadFragment;
import com.itracker.android.ui.listener.MediaPlaybackDelegate;
import com.itracker.android.utils.ConnectivityUtils;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import static com.itracker.android.utils.LogUtils.LOGE;
import static com.itracker.android.utils.LogUtils.makeLogTag;


public class MediaDownloadActivity extends BaseActivity
        implements MediaPlaybackDelegate {
    private final static String TAG = makeLogTag(MediaDownloadActivity.class);

    public final static String EXTRA_VIDEOS_TO_DOWNLOAD = "extra_videos_to_download";

    // Custom intent actions
    public final static String ACTION_DOWNLOAD_MEDIA = "com.itracker.android.intent.action.DOWNLOAD_MEDIA";

    public static Intent createVideosDownloadIntent(Context context, List<Video> videosToDownload) {
        Intent intent = new Intent(context, MediaDownloadActivity.class);
        intent.putParcelableArrayListExtra(MediaDownloadActivity.EXTRA_VIDEOS_TO_DOWNLOAD, new ArrayList<>(videosToDownload));
        return intent;
    }

    public static Intent createNotificationDownloadIntent() {
        Intent intent = new Intent(ACTION_DOWNLOAD_MEDIA);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

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
        final ArrayList<Video> videos = getIntent().getParcelableArrayListExtra(EXTRA_VIDEOS_TO_DOWNLOAD);
        Application.getInstance().runInBackground(() -> {
            try {
                requestDownloads(videos);
            } catch (Exception e) {
                LOGE(TAG, "Fail to make the download request", e);
            }
        });
    }

    private void requestDownloads(List<Video> videos) {
        if (videos != null && videos.size() > 0) {
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
                ContentResolver resolver = Application.getInstance().getContentResolver();
                resolver.applyBatch(TrackerContract.CONTENT_AUTHORITY, ops);
            } catch (RemoteException | OperationApplicationException e) {
                LOGE(TAG, "Fail to initialize new downloads: " + e);
            }
        }

        if (!ConnectivityUtils.isWifiConnected(Application.getInstance())) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), R.string.download_allowed_when_wifi_connected, Toast.LENGTH_LONG).show());
        } else {
            FileDownloadManager.getInstance().startAvailableDownloads();
        }
    }

    @Override
    public void startMediaPlayback(Uri uri, String title) {
        Intent intent = PlayerActivity.createStartPlaybackIntent(Application.getInstance(), uri, title);
        startActivity(intent);
    }
}
