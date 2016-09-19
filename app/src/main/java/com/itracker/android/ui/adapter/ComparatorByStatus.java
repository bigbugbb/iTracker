package com.itracker.android.ui.adapter;

import com.itracker.android.data.roster.AbstractContact;

public class ComparatorByStatus extends ComparatorByName {

    public static final ComparatorByStatus COMPARATOR_BY_STATUS = new ComparatorByStatus();

    @Override
    public int compare(AbstractContact object1, AbstractContact object2) {
        int result;
        result = object1.getStatusMode().compareTo(object2.getStatusMode());
        if (result != 0)
            return result;
        return super.compare(object1, object2);
    }

}