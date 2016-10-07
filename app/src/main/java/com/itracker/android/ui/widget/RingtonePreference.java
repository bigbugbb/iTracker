package com.itracker.android.ui.widget;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

/**
 * Ringtone preference that store and retrieve its data from internal property.
 *
 */
public class RingtonePreference extends android.preference.RingtonePreference {

    private Uri uri;

    public RingtonePreference(Context context) {
        super(context);
    }

    public RingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RingtonePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected Uri onRestoreRingtone() {
        return uri;
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        uri = ringtoneUri;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

}