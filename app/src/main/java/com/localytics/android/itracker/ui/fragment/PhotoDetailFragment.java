package com.localytics.android.itracker.ui.fragment;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.Photo;
import com.localytics.android.itracker.ui.listener.OnContentClickListener;

import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PhotoDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PhotoDetailFragment extends Fragment implements View.OnClickListener {
    private final static String TAG = makeLogTag(PhotoDetailFragment.class);

    private static final String ARG_CURRENT_PHOTO = "arg_current_photo";

    private Photo mPhoto;

    private ImageView mImageView;

    private OnContentClickListener mListener;

    public PhotoDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided photo.
     *
     * @param photo The current selected photo.
     * @return A new instance of fragment PhotoDetailFragment.
     */
    public static PhotoDetailFragment newInstance(Photo photo) {
        PhotoDetailFragment fragment = new PhotoDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CURRENT_PHOTO, photo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnContentClickListener) {
            mListener = (OnContentClickListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPhoto = getArguments().getParcelable(ARG_CURRENT_PHOTO);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_photo_detail, container, false);
        root.setOnClickListener(this);
        mImageView = (ImageView) root.findViewById(R.id.photo_image);
        mImageView.setOnClickListener(this);
        return root;
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onContentViewClicked(v);
        }
    }

    /**
     * Called when the fragment's activity has been created and this
     * fragment's view hierarchy instantiated.  It can be used to do final
     * initialization once these pieces are in place, such as retrieving
     * views or restoring state.  It is also useful for fragments that use
     * {@link #setRetainInstance(boolean)} to retain their instance,
     * as this callback tells the fragment when it is fully associated with
     * the new activity instance.  This is called after {@link #onCreateView}
     * and before {@link #onViewStateRestored(Bundle)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Glide.with(this)
                .load(mPhoto.data)
                .crossFade()
                .into(mImageView);
    }
}
