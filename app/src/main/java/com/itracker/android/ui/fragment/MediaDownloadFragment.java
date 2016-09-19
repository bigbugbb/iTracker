package com.itracker.android.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.itracker.android.Application;
import com.itracker.android.Config;
import com.itracker.android.R;
import com.itracker.android.data.FileDownloadManager;
import com.itracker.android.data.model.MediaDownload;
import com.itracker.android.provider.TrackerContract;
import com.itracker.android.provider.TrackerContract.DownloadStatus;
import com.itracker.android.provider.TrackerContract.FileDownloads;
import com.itracker.android.service.download.FileDownloadRequest;
import com.itracker.android.ui.listener.DownloadStateChangedListener;
import com.itracker.android.ui.widget.DownloadedMediaPropertiesDialog;
import com.itracker.android.ui.adapter.MediaDownloadAdapter;
import com.itracker.android.ui.listener.MediaPlaybackDelegate;
import com.itracker.android.utils.AppQueryHandler;
import com.itracker.android.utils.ConnectivityUtils;
import com.itracker.android.utils.ThrottledContentObserver;

import java.io.File;
import java.util.ArrayList;

import static com.itracker.android.utils.LogUtils.LOGD;
import static com.itracker.android.utils.LogUtils.makeLogTag;


public class MediaDownloadFragment extends TrackerFragment implements
        DownloadStateChangedListener,
        View.OnClickListener,
        View.OnLongClickListener {
    private static final String TAG = makeLogTag(MediaDownloadFragment.class);

    private MediaDownloadAdapter mMediaDownloadAdapter;
    private RecyclerView mMediaDownloadView;
    private TextView mEmptyView;

    private ThrottledContentObserver mMediaDownloadsObserver;

    private MediaPlaybackDelegate mMediaPlaybackDelegate;

    public MediaDownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaDownloadAdapter = new MediaDownloadAdapter(getActivity(), this, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media_download, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEmptyView = (TextView) view.findViewById(R.id.empty_view);
        mMediaDownloadView = (RecyclerView) view.findViewById(R.id.media_download_view);
        mMediaDownloadView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mMediaDownloadView.setItemAnimator(new DefaultItemAnimator());
        mMediaDownloadView.setAdapter(mMediaDownloadAdapter);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mMediaPlaybackDelegate = (MediaPlaybackDelegate) activity;

        mMediaDownloadsObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (file downloads). Content changed.");
                if (isAdded()) {
                    LOGD(TAG, "Requesting file downloads cursor reload as a result of ContentObserver firing.");
                    reloadMediaDownloads(getLoaderManager(), MediaDownloadFragment.this);
                }
            }
        });
        mMediaDownloadsObserver.setThrottleDelay(100);
        activity.getContentResolver().registerContentObserver(TrackerContract.FileDownloads.CONTENT_URI, true, mMediaDownloadsObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mMediaDownloadsObserver);
        mMediaPlaybackDelegate = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Application.getInstance().addUIListener(DownloadStateChangedListener.class, this);
        reloadMediaDownloads(getLoaderManager(), MediaDownloadFragment.this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Application.getInstance().removeUIListener(DownloadStateChangedListener.class, this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            return;
        }

        switch (loader.getId()) {
            case MediaDownloadsQuery.TOKEN_NORMAL: {
                MediaDownload[] downloads = MediaDownload.downloadsFromCursor(data);
                if (downloads != null && downloads.length > 0) {
                    mMediaDownloadAdapter.updateDownloads(downloads);
                    mEmptyView.setVisibility(View.GONE);
                } else {
                    mMediaDownloadAdapter.updateDownloads(new ArrayList<MediaDownload>());
                    mEmptyView.setVisibility(View.VISIBLE);
                }
                break;
            }
        }
    }

    private MediaDownloadAdapter.ViewHolder findViewHolderByRequestId(String requestId) {
        for (int i = 0; i < mMediaDownloadView.getChildCount(); ++i) {
            View childView = mMediaDownloadView.getChildAt(i);
            String fileId = (String) childView.getTag();
            if (TextUtils.equals(fileId, requestId)) {
                return (MediaDownloadAdapter.ViewHolder) mMediaDownloadView.getChildViewHolder(childView);
            }
        }
        return null;
    }

    @Override
    public void onPreparing(FileDownloadRequest request, Bundle extra) {
    }

    @Override
    public void onPaused(FileDownloadRequest request, Bundle extra) {
    }

    @Override
    public void onDownloading(FileDownloadRequest request, long currentFileSize, long totalFileSize,
                              long downloadSpeed, Bundle extra) {
        MediaDownloadAdapter.ViewHolder viewHolder = findViewHolderByRequestId(request.getId());
        if (viewHolder != null) {
            viewHolder.updateDownloadSpeed(downloadSpeed);
            viewHolder.updateDownloadProgress(currentFileSize, totalFileSize);
        }
    }

    @Override
    public void onCanceled(FileDownloadRequest request, Bundle extra) {
        AsyncQueryHandler handler = new AppQueryHandler(getActivity().getContentResolver());
        handler.startDelete(0, null, FileDownloads.CONTENT_URI, FileDownloads.FILE_ID + " = ?", new String[]{request.getId()});
    }

    @Override
    public void onCompleted(FileDownloadRequest request, Uri downloadedFileUri, Bundle extra) {

    }

    @Override
    public void onFailed(FileDownloadRequest request, String reason, Bundle extra) {

    }

    private void onDownloadItemClicked(MediaDownload download) {
        if (download.getStatus() == DownloadStatus.COMPLETED) {
            File downloadedFile = new File(download.local_location);
            if (downloadedFile.exists() && downloadedFile.isFile()) {
                if (mMediaPlaybackDelegate != null) {
                    mMediaPlaybackDelegate.startMediaPlayback(Uri.fromFile(downloadedFile), download.title);
                }
            } else {
                Toast.makeText(getActivity(), R.string.playback_downloaded_file_failed, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onDownloadItemLongClicked(final MediaDownload download, PopupMenu popupMenu) {;
        Menu menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu_download, menu);
        DownloadStatus status = DownloadStatus.valueOf(download.status.toUpperCase());

        if (status == DownloadStatus.PENDING || status == DownloadStatus.PREPARING ||
                status == DownloadStatus.CONNECTING || status == DownloadStatus.DOWNLOADING) {
            menu.findItem(R.id.action_pause_download).setVisible(true);
            menu.findItem(R.id.action_cancel_download).setVisible(true);
        } else if (status == DownloadStatus.PAUSED || status == DownloadStatus.FAILED) {
            menu.findItem(R.id.action_start_download).setVisible(true);
            menu.findItem(R.id.action_cancel_download).setVisible(true);
        } else if (status == DownloadStatus.COMPLETED) {
            menu.findItem(R.id.action_open_file).setVisible(true);
            menu.findItem(R.id.action_delete_file).setVisible(true);
            menu.findItem(R.id.action_show_property).setVisible(true);
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
             @Override
             public boolean onMenuItemClick(MenuItem item) {
                 switch (item.getItemId()) {
                     case R.id.action_open_file: {
                         onActionOpenFile(download);
                         break;
                     }
                     case R.id.action_delete_file: {
                         onActionDeleteFile(download);
                         break;
                     }
                     case R.id.action_start_download: {
                         onActionStartDownload(download);
                         break;
                     }
                     case R.id.action_pause_download: {
                         onActionPauseDownload(download);
                         break;
                     }
                     case R.id.action_cancel_download: {
                         onActionCancelDownload(download);
                         break;
                     }
                     case R.id.action_show_property: {
                         onActionShowProperty(download);
                         break;
                     }
                 }
                 return true;
             }
         });

        popupMenu.show();
    }

    private void onActionOpenFile(MediaDownload download) {
        if (!TextUtils.isEmpty(download.local_location)) {
            if (mMediaPlaybackDelegate != null) {
                mMediaPlaybackDelegate.startMediaPlayback(Uri.fromFile(new File(download.local_location)), download.title);
            }
        }
    }

    private void onActionDeleteFile(final MediaDownload download) {
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                // Set Dialog Icon
                .setIcon(R.mipmap.ic_launcher)
                // Set Dialog Title
                .setTitle("Delete file")
                // Set Dialog Message
                .setMessage(R.string.delete_file_prompt)

                // Positive button
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        AsyncQueryHandler handler = new AppQueryHandler(getActivity().getContentResolver()) {
                            @Override
                            protected void onDeleteComplete(int token, Object cookie, int result) {
                                deleteDownloadedFile((String) cookie);
                            }
                        };
                        handler.startDelete(0, download.local_location, FileDownloads.CONTENT_URI, FileDownloads.FILE_ID + " = ?", new String[]{download.identifier});
                    }
                })

                // Negative Button
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,	int which) {
                        // Do nothing
                    }
                }).create();

        dialog.show();
    }

    private void onActionShowProperty(MediaDownload download) {
        DownloadedMediaPropertiesDialog dialog = DownloadedMediaPropertiesDialog.getInstance(download);
        if (getFragmentManager().findFragmentByTag(DownloadedMediaPropertiesDialog.TAG) == null) {
            dialog.show(getFragmentManager(), DownloadedMediaPropertiesDialog.TAG);
        }
    }

    private void onActionStartDownload(MediaDownload download) {
        Context context = getActivity().getApplicationContext();
        if (!ConnectivityUtils.isWifiConnected(context)) {
            Toast.makeText(getActivity(), R.string.download_allowed_when_wifi_connected, Toast.LENGTH_LONG).show();
        } else {
            FileDownloadManager.getInstance().startDownload(download.identifier);
        }
    }

    private void onActionPauseDownload(MediaDownload download) {
        FileDownloadManager.getInstance().pauseDownload(download.identifier);
    }

    private void onActionCancelDownload(final MediaDownload download) {
        Context context = Application.getInstance();
        final String tmpFileLocation = Config.FILE_DOWNLOAD_DIR_PATH + download.identifier; // In the future if no default location is used, the download object should also contain the destination location

        SpannableString statusText = new SpannableString(
                getString(R.string.cancel_download_prompt) + "\n" + getString(R.string.remove_downloaded_data_warning));
        statusText.setSpan(new ForegroundColorSpan(
                ContextCompat.getColor(context, android.R.color.holo_red_light)),
                getString(R.string.cancel_download_prompt).length(),
                statusText.length(),
                0);
        statusText.setSpan(new StyleSpan(
                Typeface.ITALIC),
                getString(R.string.cancel_download_prompt).length(),
                statusText.length(),
                0);

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                // Set Dialog Icon
                .setIcon(R.mipmap.ic_launcher)
                // Set Dialog Title
                .setTitle("Cancel download")
                // Set Dialog Message
                .setMessage(statusText)
                // Positive button
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        FileDownloadManager.getInstance().cancelDownload(download.identifier);
                    }
                })
                // Negative Button
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,	int which) {
                        // Do nothing
                    }
                })
                .create();

        DownloadStatus status = download.getStatus();
        if (status == DownloadStatus.PENDING || status == DownloadStatus.PREPARING ||
                status == DownloadStatus.CONNECTING || status == DownloadStatus.DOWNLOADING) {
            dialog.show();
        } else if (status == DownloadStatus.FAILED || status == DownloadStatus.PAUSED) {
            AsyncQueryHandler handler = new AppQueryHandler(getActivity().getContentResolver()) {
                @Override
                protected void onDeleteComplete(int token, Object cookie, int result) {
                    deleteDownloadedFile((String) cookie);
                }
            };
            handler.startDelete(0, tmpFileLocation, FileDownloads.CONTENT_URI, FileDownloads.FILE_ID + " = ?", new String[]{download.identifier});
        }
    }

    private void deleteDownloadedFile(String fileLocation) {
        if (!TextUtils.isEmpty(fileLocation)) {
            File downloadedFile = new File(fileLocation);
            if (downloadedFile.exists() && downloadedFile.isFile()) {
                downloadedFile.delete();
            }
        }
    }

    @Override
    public void onClick(View itemView) {
        MediaDownload download = mMediaDownloadAdapter.getItem((String) itemView.getTag());
        onDownloadItemClicked(download);
    }

    @Override
    public boolean onLongClick(View itemView) {
        TextView downloadFileSize = (TextView) itemView.findViewById(R.id.media_file_size);
        MediaDownload download = mMediaDownloadAdapter.getItem((String) itemView.getTag());
        PopupMenu popupMenu = new PopupMenu(getActivity(), downloadFileSize);
        onDownloadItemLongClicked(download, popupMenu);
        return true;
    }
}
