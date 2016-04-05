package com.localytics.android.itracker.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.localytics.android.itracker.R;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;


public class FriendFragment extends TrackerFragment {
    private static final String TAG = makeLogTag(FriendFragment.class);

    private ListView mFriendsView;

    public FriendFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_friend, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFriendsView = (ListView) view.findViewById(R.id.friends_view);
        ViewGroup footer = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.friends_list_footer, mFriendsView, false);
        mFriendsView.addFooterView(footer, null, false);
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
