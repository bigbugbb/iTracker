package com.localytics.android.itracker.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.api.services.youtube.model.Video;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.localytics.android.itracker.R;

import java.util.ArrayList;
import java.util.List;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class MediaDownloadFragment extends Fragment {

    private static final String TAG = makeLogTag(MediaDownloadFragment.class);

    private RecyclerView mDownloadsView;
    private LinearLayoutManager mLayoutManager;

    private List<Video> mSelectedVideos = new ArrayList<>();

    private static final String ARG_VIDEOS_TO_DOWNLOAD = "arg_videos_to_download";

    public static MediaDownloadFragment newInstance(String jsonVideosToDownload) {
        MediaDownloadFragment fragment = new MediaDownloadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VIDEOS_TO_DOWNLOAD, jsonVideosToDownload);
        fragment.setArguments(args);
        return fragment;
    }

    public MediaDownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            final String jsonSelectedVideos = args.getString(ARG_VIDEOS_TO_DOWNLOAD);
            if (!TextUtils.isEmpty(jsonSelectedVideos)) {
                mSelectedVideos = new Gson().fromJson(jsonSelectedVideos, new TypeToken<List<Video>>(){}.getType());
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
