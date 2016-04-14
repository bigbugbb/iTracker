package com.localytics.android.itracker.data.model;


public abstract class BaseData implements Comparable<BaseData> {
    public long time;
    public long track_id;

    /**
     * Compares this object to the specified object to determine their relative
     * order.
     *
     * @param another the object to compare to this instance.
     * @return a negative integer if this instance is less than {@code another};
     * a positive integer if this instance is greater than
     * {@code another}; 0 if this instance has the same order as
     * {@code another}.
     * @throws ClassCastException if {@code another} cannot be converted into something
     *                            comparable to {@code this} instance.
     */
    @Override
    public int compareTo(BaseData another) {
        return (int) (time - another.time);
    }

    abstract public String[] convertToCsvLine();
}
