package com.itracker.android.ui.fragment.preferences;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import com.itracker.android.R;

public class PriorityPreference extends EditTextPreference {

    private final Context context;

    public PriorityPreference(Context context) {
        super(context);
        this.context = context;
    }

    public PriorityPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public PriorityPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    @Override
    protected boolean callChangeListener(Object newValue) {
        try {
            int value = Integer.parseInt((String) newValue);
            if (value < -128 || value > 128)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(context,
                    context.getString(R.string.account_invalid_priority),
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return super.callChangeListener(newValue);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        String summary = text;
        try {
            if (Integer.parseInt(text) < 0)
                summary = context.getString(R.string.negative_priotiry_summary,
                        text);
        } catch (NumberFormatException e) {
        }
        setSummary(summary);
    }

}