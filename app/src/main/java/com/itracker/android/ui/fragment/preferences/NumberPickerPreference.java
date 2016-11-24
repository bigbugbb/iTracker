package com.itracker.android.ui.fragment.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import com.itracker.android.R;

public class NumberPickerPreference extends DialogPreference {

    private int mMin;
    private int mMax;
    private int mStep;

    private int mNumber;

    /**
     * Resource of the dialog layout
     */
    private int mDialogLayoutResId;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.NumberPickerPreference, defStyleAttr, defStyleRes);

        mMin = a.getInt(R.styleable.NumberPickerPreference_min, 0);
        mMax = a.getInt(R.styleable.NumberPickerPreference_max, 100);
        mStep = a.getInt(R.styleable.NumberPickerPreference_step, 1);
        mDialogLayoutResId = a.getResourceId(R.styleable.NumberPickerPreference_dialog_layout, R.layout.pref_dialog_number);

        a.recycle();

        setDialogLayoutResource(mDialogLayoutResId);
        setDialogIcon(null);
    }

    public int getMin() {
        return mMin;
    }

    public int getMax() {
        return mMax;
    }

    public int getStep() {
        return mStep;
    }

    /**
     * Gets the number from the Shared Preferences
     *
     * @return The current preference value
     */
    public int getNumber() {
        return mNumber;
    }

    /**
     * Saves the number to the SharedPreferences
     *
     * @param number The number to save
     */
    public void setNumber(int number) {
        mNumber = number;

        // Save to SharedPreference
        persistInt(number);
    }

    /**
     * Called when a Preference is being inflated and the default value attribute needs to be read
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // The type of this preference is Int, so we read the default value from the attributes
        // as Int. Fallback value is set to 0.
        return a.getInt(index, 0);
    }

    /**
     * Returns the layout resource that is used as the content View for the dialog
     */
    @Override
    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }

    /**
     * Implement this to set the initial value of the Preference.
     */
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        // If the value can be restored, do it. If not, use the default value.
        setNumber(restorePersistedValue ? getPersistedInt(mNumber) : (int) defaultValue);
    }
}