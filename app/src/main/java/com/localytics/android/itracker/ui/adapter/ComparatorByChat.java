package com.localytics.android.itracker.ui.adapter;


import com.localytics.android.itracker.data.message.AbstractChat;
import com.localytics.android.itracker.data.message.MessageManager;
import com.localytics.android.itracker.data.roster.AbstractContact;

public class ComparatorByChat extends ComparatorByName {

    public static final ComparatorByChat COMPARATOR_BY_CHAT = new ComparatorByChat();

    @Override
    public int compare(AbstractContact object1, AbstractContact object2) {
        final MessageManager messageManager = MessageManager.getInstance();
        final AbstractChat abstractChat1 = messageManager.getChat(
                object1.getAccount(), object1.getUser());
        final AbstractChat abstractChat2 = messageManager.getChat(
                object2.getAccount(), object2.getUser());
        final boolean hasActiveChat1 = abstractChat1 != null
                && abstractChat1.isActive();
        final boolean hasActiveChat2 = abstractChat2 != null
                && abstractChat2.isActive();
        if (hasActiveChat1 && !hasActiveChat2)
            return -1;
        if (!hasActiveChat1 && hasActiveChat2)
            return 1;
        if (hasActiveChat1) {
            int result;
            result = ChatComparator.CHAT_COMPARATOR.compare(abstractChat1,
                    abstractChat2);
            if (result != 0)
                return result;
        }
        return super.compare(object1, object2);
    }

}