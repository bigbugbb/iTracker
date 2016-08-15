package com.localytics.android.xmpp.blocking;


import com.localytics.android.xmpp.AbstractIQProvider;

import org.jivesoftware.smack.SmackException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

abstract class BasicBlockingProvider<BlockingIq extends BasicBlockingIq> extends AbstractIQProvider<BlockingIq> {

    @Override
    protected boolean parseInner(XmlPullParser parser, BlockingIq instance) throws XmlPullParserException, IOException, SmackException {
        if (super.parseInner(parser, instance)) {
            return true;
        }

        if (parser.getName().equalsIgnoreCase(XmlConstants.ITEM)) {

            final String item = parser.getAttributeValue(null, XmlConstants.ITEM_JID);
            if (item != null) {
                instance.addItem(item);
            }
            parser.next();
        }

        return true;
    }
}
