package com.localytics.android.itracker.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.List;

import static com.localytics.android.itracker.utils.LogUtils.makeLogTag;

public abstract class BaseRecyclerAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = makeLogTag(BaseRecyclerAdapter.class);

    protected final Context mContext;
    protected final LayoutInflater mLayoutInflater;

    protected List<T> mData = new ArrayList<>();

    public BaseRecyclerAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    public List<T> getData() {
        return mData;
    }

    public T getItem(int position) {
        return position < mData.size() ? mData.get(position) : null;
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    /**
     * 移除某一条记录
     *
     * @param position 移除数据的position
     */
    public void removeItem(int position) {
        if (position < mData.size()) {
            mData.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * 添加一条记录
     *
     * @param item     需要加入的数据结构
     * @param position 插入位置
     */
    public void addItem(T item, int position) {
        if (position <= mData.size()) {
            mData.add(position, item);
            notifyItemInserted(position);
        }
    }

    /**
     * 添加一条记录
     *
     * @param item 需要加入的数据结构
     */
    public void addItem(T item) {
        addItem(item, mData.size());
    }

    /**
     * 移除所有记录
     */
    public void clearItems() {
        int size = mData.size();
        if (size > 0) {
            mData.clear();
            notifyItemRangeRemoved(0, size);
        }
    }

    /**
     * 批量添加记录
     *
     * @param items    需要加入的数据结构
     * @param position 插入位置
     */
    public void addItems(List<T> items, int position) {
        if (position <= mData.size() && items != null && items.size() > 0) {
            mData.addAll(position, items);
            notifyItemRangeChanged(position, items.size());
        }
    }

    /**
     * 批量添加记录
     *
     * @param items 需要加入的数据结构
     */
    public void addItems(List<T> items) {
        addItems(items, mData.size());
    }
}