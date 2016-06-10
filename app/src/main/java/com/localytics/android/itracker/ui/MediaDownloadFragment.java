package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Video;

import java.util.List;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class MediaDownloadFragment extends Fragment {
    private static final String TAG = makeLogTag(MediaDownloadFragment.class);

    private RecyclerView mDownloadsView;
    private LinearLayoutManager mLayoutManager;

    private List<Video> mVideosToDownload;

    public MediaDownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getActivity().getIntent();
        mVideosToDownload = intent.getParcelableArrayListExtra(MediaDownloadActivity.EXTRA_VIDEOS_TO_DOWNLOAD);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media_download, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
