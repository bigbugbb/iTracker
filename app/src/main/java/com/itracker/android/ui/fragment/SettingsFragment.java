package com.itracker.android.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.widget.Toast;

import com.itracker.android.R;
import com.itracker.android.ui.dialog.NumberPickerPreferenceDialog;
import com.itracker.android.ui.fragment.preferences.NumberPickerPreference;
import com.itracker.android.utils.HelpUtils;

public class SettingsFragment extends PreferenceFragmentCompat {

    private int mClickCount;
    private Handler mHandler = new Handler();
    private static final String EMOJI_WITH_ABOUT = "\uD83D\uDE0E";

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // Load the Preferences from the XML file
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        super.onPreferenceTreeClick(preference);
        if (preference.getKey().equals(getString(R.string.about_key))) {
//            if (++mClickCount == 3) {
//                Toast.makeText(getActivity(), EMOJI_WITH_ABOUT, Toast.LENGTH_SHORT).show();
//            }
//            mHandler.removeCallbacksAndMessages(null);
//            mHandler.postDelayed(() -> mClickCount = 0, 1000);
            HelpUtils.showAbout(getActivity());
        }
        return false;
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        // Try if the preference is one of our custom Preferences
        DialogFragment dialogFragment = null;
        if (preference instanceof NumberPickerPreference) {
            // Create a new instance of TimePreferenceDialogFragment with the key of the related
            // Preference
            dialogFragment = NumberPickerPreferenceDialog.newInstance(preference.getKey());
        }

        if (dialogFragment != null) {
            // The dialog was created (it was one of our custom Preferences), show the dialog for it
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
        } else {
            // Dialog creation could not be handled here. Try with the super method.
            super.onDisplayPreferenceDialog(preference);
        }
    }
}