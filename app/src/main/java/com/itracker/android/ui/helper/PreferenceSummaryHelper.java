package com.itracker.android.ui.helper;

import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;

/**
 * Update preference's title and summary based on multiline title.
 *
 */
public final class PreferenceSummaryHelper {

    private PreferenceSummaryHelper() {
    }

    public static void updateSummary(PreferenceGroup group) {
        for (int index = 0; index < group.getPreferenceCount(); index++) {
            Preference preference = group.getPreference(index);
            if (preference instanceof PreferenceGroup) {
                updateSummary((PreferenceGroup) preference);
            }
            String titleAndSummary = preference.getTitle().toString();

            if (!isTitleAndSummary(titleAndSummary)) {
                continue;
            }

            preference.setTitle(getPreferenceTitle(titleAndSummary));
            if (preference instanceof DialogPreference) {
                ((DialogPreference) preference).setDialogTitle(preference.getTitle());
            }
            preference.setSummary(getPreferenceSummary(titleAndSummary));
        }
    }

    private static boolean isTitleAndSummary(String titleAndSummary) {
        return titleAndSummary.contains("\n");
    }

    public static String getPreferenceTitle(String titleAndSummary) {
        int delimiter = titleAndSummary.indexOf("\n");
        if (delimiter == -1) {
            return titleAndSummary;
        }
        return titleAndSummary.substring(0, delimiter);
    }

    private static String getPreferenceSummary(String titleAndSummary) {
        int delimiter = titleAndSummary.indexOf("\n");
        if (delimiter == -1) {
            return "";
        }
        return titleAndSummary.substring(delimiter + 1, titleAndSummary.length());
    }

}