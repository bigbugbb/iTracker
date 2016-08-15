package com.localytics.android.itracker.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.User;
import com.localytics.android.itracker.ui.widget.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;


public class FriendFragment extends TrackerFragment {
    private static final String TAG = makeLogTag(FriendFragment.class);

    private RecyclerView   mFriendsView;
    private FriendsAdapter mFriendsAdapter;
    private LinearLayoutManager mLayoutManager;

    private ProgressBar mProgressView;

    public FriendFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPosition = 3;

        mFriendsAdapter = new FriendsAdapter(getActivity());
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

        mLayoutManager = new LinearLayoutManager(getActivity());

        mFriendsView = (RecyclerView) view.findViewById(R.id.friends_view);
        mFriendsView.setLayoutManager(mLayoutManager);
        mFriendsView.setItemAnimator(new DefaultItemAnimator());
        mFriendsView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
        mFriendsView.setAdapter(mFriendsAdapter);

        mProgressView = (ProgressBar) view.findViewById(R.id.progress_view);
        mProgressView.setVisibility(mFriendsAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
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

    private class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

        private Context mContext;
        private List<User> mFriends = new ArrayList<>();

        public FriendsAdapter(Context context) {
            mContext = context;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(mContext).inflate(R.layout.item_friend, parent, false);
            return new ViewHolder(item);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bindData(mFriends.get(position));
        }

        @Override
        public int getItemCount() {
            return mFriends.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnail;
            TextView  username;
            TextView  message;
            TextView  messageTime;

            public ViewHolder(View itemView) {
                super(itemView);
                thumbnail   = (ImageView) itemView.findViewById(R.id.friend_thumbnail);
                username    = (TextView) itemView.findViewById(R.id.friend_username);
                message     = (TextView) itemView.findViewById(R.id.friend_message);
                messageTime = (TextView) itemView.findViewById(R.id.friend_message_time);
            }

            public void bindData(final User user) {

            }
        }
    }
}
