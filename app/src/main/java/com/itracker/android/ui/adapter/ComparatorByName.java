package com.itracker.android.ui.adapter;

import com.itracker.android.data.roster.AbstractContact;

import java.util.Comparator;

public class ComparatorByName implements Comparator<AbstractContact> {

    public static final ComparatorByName COMPARATOR_BY_NAME = new ComparatorByName();

    @Override
    public int compare(AbstractContact object1, AbstractContact object2) {
        int result;
        result = object1.getName().compareToIgnoreCase(object2.getName());
        if (result != 0)
            return result;
        return object1.getAccount().compareToIgnoreCase(object2.getAccount());
    }

}