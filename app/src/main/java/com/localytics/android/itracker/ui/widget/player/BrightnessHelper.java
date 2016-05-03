package com.localytics.android.itracker.ui.widget.player;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

public class BrightnessHelper {

    public static void setBrightness(Context context, int brightness){
        ContentResolver resolver = context.getContentResolver();
//        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
    }

    public static int getBrightness(Context context) {
        ContentResolver resolver = context.getContentResolver();
        try {
            return Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            return 0;
        }
    }
}
