package com.itracker.android.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.model.Track;
import com.itracker.android.ui.listener.OnTrackItemSelectedListener;

import org.joda.time.DateTime;


public class TrackItemAdapter extends BaseAbstractRecyclerCursorAdapter<TrackItemAdapter.ViewHolder> {
    private Context mContext;

    public TrackItemAdapter(Context context) {
        super(context, null);
        mContext = context;
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
            for (OnTrackItemSelectedListener listener :
                    Application.getInstance().getUIListeners(OnTrackItemSelectedListener.class)) {
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
            view.setOnClickListener(v -> {
                for (OnTrackItemSelectedListener listener :
                        Application.getInstance().getUIListeners(OnTrackItemSelectedListener.class)) {
                    listener.onTrackItemSelected(view, getLayoutPosition());
                }
            });
            mDate = (TextView) view.findViewById(R.id.track_date);
        }

        void bindData(final Track track) {
            DateTime dateTime = new DateTime(track.date);
            String date = new StringBuilder()
                    .append(dateTime.year().getAsText())
                    .append(" ")
                    .append(dateTime.monthOfYear().getAsShortText())
                    .append(" ")
                    .append(dateTime.dayOfMonth().getAsText())
                    .append(", ")
                    .append(dateTime.dayOfWeek().getAsText())
                    .toString();
            if (mDate != null) {
                mDate.setText(date);
            }
        }
    }
}