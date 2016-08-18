package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.widget.Toast;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.MediaDownload;
import com.localytics.android.itracker.data.model.Video;
import com.localytics.android.itracker.data.FileDownloadManager;
import com.localytics.android.itracker.provider.TrackerContract;
import com.localytics.android.itracker.provider.TrackerContract.DownloadStatus;
import com.localytics.android.itracker.provider.TrackerContract.FileDownloads;
import com.localytics.android.itracker.utils.ConnectivityUtils;

import org.joda.time.DateTime;

import java.util.ArrayList;

import static com.localytics.android.itracker.utils.LogUtils.LOGE;
import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;


public class MediaDownloadActivity extends BaseActivity implements MediaDownloadFragment.OnStartMediaPlaybackListener {
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

        mRequestHandler = new RequestHandler(getApplicationContext(), mRequestThread.getLooper());
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
        if (videos == null || videos.size() == 0) {
            return;
        }
        mRequestHandler.obtainMessage(MESSAGE_REQUEST_DOWNLOADS, videos).sendToTarget(); // AsyncTask leads to bad response time in this case
    }

    private static final int MESSAGE_REQUEST_DOWNLOADS = 100;

    private class RequestHandler extends Handler {

        private Context mContext;

        public RequestHandler(Context context, Looper looper) {
            super(looper);
            mContext = context;
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                ArrayList<Video> videos = (ArrayList) msg.obj;
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
                    ContentResolver resolver = mContext.getContentResolver();
                    resolver.applyBatch(TrackerContract.CONTENT_AUTHORITY, ops);
                } catch (RemoteException | OperationApplicationException e) {
                    LOGE(TAG, "Fail to initialize new downloads: " + e);
                }

                if (!ConnectivityUtils.isWifiConnected(mContext)) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), R.string.download_allowed_when_wifi_connected, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    FileDownloadManager.getInstance().startAvailableDownloads();
                }
            } catch (Exception e) {
                LOGE(TAG, "Fail to make the download request", e);
            }
        }
    }

    @Override
    public void onStartMediaPlayback(Uri uri, MediaDownload download) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.setData(uri);
        intent.putExtra(PlayerActivity.MEDIA_PLAYER_TITLE, download.title);
        startActivity(intent);
    }
}
