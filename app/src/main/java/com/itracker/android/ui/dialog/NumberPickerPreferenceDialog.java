package com.itracker.android.ui.dialog;

import android.os.Bundle;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.NumberPicker;

import com.itracker.android.R;
import com.itracker.android.ui.fragment.preferences.NumberPickerPreference;

/**
 * Created by bigbug on 11/23/16.
 */

public class NumberPickerPreferenceDialog extends PreferenceDialogFragmentCompat {

    /**
     * The NumberPicker widget
     */
    private NumberPicker mNumberPicker;

    /**
     * Creates a new Instance of the NumberPickerPreferenceDialog and stores the key of the
     * related Preference
     *
     * @param key The key of the related Preference
     * @return A new Instance of the NumberPickerPreferenceDialog
     */
    public static NumberPickerPreferenceDialog newInstance(String key) {
        final NumberPickerPreferenceDialog
                fragment = new NumberPickerPreferenceDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);

        // Exception: There is no TimePicker with the id 'edit' in the dialog.
        if (mNumberPicker == null) {
            throw new IllegalStateException("Dialog view must contain a NumberPicker with id 'edit'");
        }

        // Get the time from the related Preference
        Integer number;
        DialogPreference preference = getPreference();
        if (preference instanceof NumberPickerPreference) {
            NumberPickerPreference numberPickerPreference = (NumberPickerPreference) preference;
            number = numberPickerPreference.getNumber();
            int step = numberPickerPreference.getStep();
            int min = numberPickerPreference.getMin() / step;
            int max = numberPickerPreference.getMax() / step;

            String[] displayedValues = new String[max - min + 1];
            for (int i = min; i <= max; ++i) {
                displayedValues[i - min] = String.valueOf(step * i);
            }

            mNumberPicker.setMinValue(min);
            mNumberPicker.setMaxValue(max);
            mNumberPicker.setDisplayedValues(displayedValues);
            mNumberPicker.setWrapSelectorWheel(false);

            if (number != null) mNumberPicker.setValue(number);
        }
    }

    /**
     * Called when the Dialog is closed.
     *
     * @param positiveResult Whether the Dialog was accepted or canceled.
     */
    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mNumberPicker.clearFocus();

            // Save the value
            DialogPreference preference = getPreference();
            if (preference instanceof NumberPickerPreference) {
                NumberPickerPreference numberPickerPreference = (NumberPickerPreference) preference;

                // Get the current value from the NumberPicker
                int number = mNumberPicker.getValue();

                // This allows the client to ignore the user value.
                if (numberPickerPreference.callChangeListener(number)) {
                    // Save the value
                    numberPickerPreference.setNumber(number);
                }
            }
        }
    }
}
