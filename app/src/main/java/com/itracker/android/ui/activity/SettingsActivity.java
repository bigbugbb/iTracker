package com.itracker.android.ui.activity;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.itracker.android.R;
import com.itracker.android.ui.dialog.NumberPickerPreferenceDialog;
import com.itracker.android.ui.fragment.SettingsFragment;
import com.itracker.android.ui.fragment.preferences.NumberPickerPreference;


public class SettingsActivity extends BaseActivity {

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        Fragment fragment = getFragmentManager().findFragmentById(R.id.settings_fragment);
        if (fragment == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_fragment, new SettingsFragment())
                    .commit();
        }
    }
}
