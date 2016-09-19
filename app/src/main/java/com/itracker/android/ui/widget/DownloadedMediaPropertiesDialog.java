package com.itracker.android.ui.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.itracker.android.Config;
import com.itracker.android.R;
import com.itracker.android.data.model.MediaDownload;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import static com.itracker.android.utils.LogUtils.makeLogTag;

public class DownloadedMediaPropertiesDialog extends DialogFragment {
    public static final String TAG = makeLogTag(DownloadedMediaPropertiesDialog.class);

    private TextView mFileTitle;
    private TextView mFilePath;
    private TextView mFileSize;
    private TextView mFileFinished;

    private MediaDownload mDownload;

    private final static String PATTERN = "E MM/dd/yyyy HH:mm:ss";

    private final static String ARGS_MEDIA_DOWNLOAD = "args_media_download";

    public static DownloadedMediaPropertiesDialog getInstance(MediaDownload download) {
        DownloadedMediaPropertiesDialog dialog = new DownloadedMediaPropertiesDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARGS_MEDIA_DOWNLOAD, download);
        dialog.setArguments(bundle);
        return dialog;
    }

    public DownloadedMediaPropertiesDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDownload = getArguments().getParcelable(ARGS_MEDIA_DOWNLOAD);
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.media_dialog_title)
                .setView(getCustomView(getActivity()))
                .create();
    }

    private View getCustomView(Context context) {
        View container = LayoutInflater.from(context).inflate(R.layout.dialog_downloaded_media_properties, null);
        mFileTitle = (TextView) container.findViewById(R.id.media_file_title);
        mFilePath  = (TextView) container.findViewById(R.id.media_file_path);
        mFileSize  = (TextView) container.findViewById(R.id.media_file_size);
        mFileFinished = (TextView) container.findViewById(R.id.media_file_finished);

        if (mDownload != null) {
            DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
            DateTime dt = formatter.parseDateTime(mDownload.finish_time);

            mFileTitle.setText(mDownload.title);
            mFilePath.setText(Config.FILE_DOWNLOAD_DIR_PATH + mDownload.identifier);
            mFileSize.setText(formatFileTotalSize(mDownload.total_size));
            mFileFinished.setText(dt.toString(PATTERN));
        }

        return container;
    }

    private String formatFileTotalSize(long totalSize) {
        float sizeInMb = totalSize / 1024f / 1024f;
        if (sizeInMb < 1) {
            return String.format("%.2f MB (%d Bytes)", sizeInMb, totalSize);
        } else if (sizeInMb > 1024) {
            float sizeInGb = totalSize / 1024f;
            return String.format("%.2f GB (%d Bytes)", sizeInGb, totalSize);
        }
        return String.format("%.1f MB (%d Bytes)", sizeInMb, totalSize);
    }
}