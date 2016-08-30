package com.localytics.android.itracker.ui;

import com.localytics.android.itracker.data.message.AbstractChat;

import java.util.Comparator;

public class ChatComparator implements Comparator<AbstractChat> {

    public static final ChatComparator CHAT_COMPARATOR = new ChatComparator();

    @Override
    public int compare(AbstractChat object1, AbstractChat object2) {
        if (object1.getLastTime() == null) {
            if (object2.getLastTime() != null)
                return 1;
            return 0;
        } else {
            if (object2.getLastTime() == null)
                return -1;
            return -object1.getLastTime().compareTo(object2.getLastTime());
        }
    }

}