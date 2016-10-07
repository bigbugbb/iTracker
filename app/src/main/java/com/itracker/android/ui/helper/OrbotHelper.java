package com.itracker.android.ui.helper;

import android.content.pm.PackageManager;

import com.itracker.android.Application;

public class OrbotHelper {

    public final static String URI_ORBOT = "org.torproject.android";

    public static boolean isOrbotInstalled() {
        PackageManager packageManager = Application.getInstance()
                .getPackageManager();
        try {
            packageManager.getPackageInfo(URI_ORBOT,
                    PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

}
