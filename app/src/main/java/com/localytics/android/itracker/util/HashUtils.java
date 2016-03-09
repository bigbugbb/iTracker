package com.localytics.android.itracker.util;

import java.util.Locale;

public class HashUtils {
    public static String computeWeakHash(String string) {
        return String.format(Locale.US, "%08x%08x", string.hashCode(), string.length());
    }
}