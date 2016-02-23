package com.localytics.android.itracker.ui.widget;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.adapter.BaseAbstractRecyclerCursorAdapter;
import com.localytics.android.itracker.data.model.Track;
import com.localytics.android.itracker.ui.OnTrackItemSelectedListener;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class TrackItemAdapter extends BaseAbstractRecyclerCursorAdapter<TrackItemAdapter.ViewHolder> {
    private Context mContext;
    private List<OnTrackItemSelectedListener> mListeners = new ArrayList<>();

    public TrackItemAdapter(Context context) {
        super(context, null);
        mContext = context;
    }

    public void addOnItemSelectedListener(OnTrackItemSelectedListener listener) {
        mListeners.add(listener);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(mContext).inflate(R.layout.item_track, parent, false);
        return new ViewHolder(item);
    }

    /**
     * Call when bind view with the cursor
     *
     * @param holder RecyclerView.ViewHolder
     * @param cursor The cursor from which to get the data. The cursor is already
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
        Track track = new Track(cursor);
        holder.bindData(track);

        if (cursor.isFirst()) {
            for (OnTrackItemSelectedListener listener : mListeners) {
                listener.onTrackItemSelected(holder.itemView, holder.getLayoutPosition());
            }
        }
    }

    @Override
    public Track getItem(int position) {
        Cursor cursor = (Cursor) super.getItem(position);
        if (cursor != null) {
            return new Track(cursor);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView mDate;

        ViewHolder(final View view) {
            super(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (OnTrackItemSelectedListener listener : mListeners) {
                        listener.onTrackItemSelected(view, getLayoutPosition());
                    }
                }
            });
            mDate = (TextView) view.findViewById(R.id.track_date);
        }

        void bindData(final Track track) {
            DateTime dateTime = new DateTime(track.date);
            String date = new StringBuilder()
                    .append(dateTime.year().getAsText())
                    .append(", ")
                    .append(dateTime.monthOfYear().getAsText())
                    .append(" ")
                    .append(dateTime.dayOfMonth().getAsText())
                    .append(", ")
                    .append(dateTime.dayOfWeek().getAsShortText())
                    .toString();
            mDate.setText(date);
        }
    }
}