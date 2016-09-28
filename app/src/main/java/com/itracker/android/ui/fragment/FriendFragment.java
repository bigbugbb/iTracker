package com.itracker.android.ui.fragment;


import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.itracker.android.R;
import com.itracker.android.ui.listener.OnSelectedStateChangedListener;

import static com.itracker.android.utils.LogUtils.makeLogTag;

public class FriendFragment extends TrackerFragment {
    private static final String TAG = makeLogTag(FriendFragment.class);

    public FriendFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPosition = 3;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend, container, false);

        ViewPager pager = (ViewPager) view.findViewById(R.id.pager);
        pager.setAdapter(new FriendFragmentPagerAdapter(getActivity(), getChildFragmentManager()));

        TabLayout tabs = (TabLayout) view.findViewById(R.id.tabs_friend);
        tabs.setupWithViewPager(pager);

        return view;
    }

    @Override
    public void onSelected() {

    }

    @Override
    public void onUnselected() {

    }

    private class FriendFragmentPagerAdapter extends FragmentPagerAdapter {
        final Context mContext;

        public FriendFragmentPagerAdapter(Context context, FragmentManager manager) {
            super(manager);
            mContext = context;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new ChatListFragment();
                case 1:
                    return new ContactListFragment();
                default:
                    return null;
            }
        }

        @Override
        public String getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.friend_tab_name_messages);
                case 1:
                    return getString(R.string.friend_tab_name_contacts);
                default:
                    return null;
            }
        }
    }
}
