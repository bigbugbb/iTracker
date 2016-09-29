package com.itracker.android.ui.widget;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.itracker.android.Config;
import com.itracker.android.R;
import com.itracker.android.ui.fragment.TrackerFragment;
import com.itracker.android.utils.PrefUtils;

import org.joda.time.DateTime;

import static com.itracker.android.utils.LogUtils.makeLogTag;


public class TimeRangeController {

    private static final String TAG = makeLogTag(TimeRangeController.class);

    private TrackerFragment mFragment;

    private View mTimeRange;

    private TextView mBeginText;
    private TextView mEndText;

    private DateTime mBeginDate;
    private DateTime mEndDate;

    private final static String BEGIN_DATE = "begin_date";
    private final static String END_DATE = "end_date";

    public TimeRangeController(TrackerFragment fragment) {
        mFragment = fragment;
    }

    public void create() {
        mTimeRange = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.search_date_range, null);
        mBeginText = (TextView) mTimeRange.findViewById(R.id.begin_date);
        mBeginText.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    mFragment.getActivity(),
                    0,
                    mBeginDateSetListener,
                    mBeginDate.getYear(),
                    mBeginDate.getMonthOfYear() - 1,
                    mBeginDate.getDayOfMonth());
            int[] location = new int[2];
            mBeginText.getLocationOnScreen(location);
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.y = location[1];
            dialog.show();
        });
        mEndText = (TextView) mTimeRange.findViewById(R.id.end_date);
        mEndText.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    mFragment.getActivity(),
                    0,
                    mEndDateSetListener,
                    mEndDate.getYear(),
                    mEndDate.getMonthOfYear() - 1,
                    mEndDate.getDayOfMonth());

            int[] location = new int[2];
            mEndText.getLocationOnScreen(location);
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.y = location[1];
            dialog.show();
        });

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mFragment.getActivity());
        mEndDate = new DateTime(sp.getLong(END_DATE,
                DateTime.now().plusDays(1).withTimeAtStartOfDay().minusSeconds(1).getMillis()));
        mBeginDate = new DateTime(sp.getLong(BEGIN_DATE,
                mEndDate.minusDays(Config.DEFAULT_DAYS_BACK_FROM_TODAY + 1).plusSeconds(1).getMillis()));
        updateOutdatedTimeRange();

        setDateText(mBeginText, mBeginDate);
        setDateText(mEndText, mEndDate);
    }

    public void saveState() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mFragment.getActivity());
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(BEGIN_DATE, mBeginDate.getMillis());
        editor.putLong(END_DATE, mEndDate.getMillis());
        editor.apply();
    }

    public View getTimeRange() {
        return mTimeRange;
    }

    public DateTime getBeginDate() {
        return mBeginDate;
    }

    public DateTime getEndDate() {
        return mEndDate;
    }

    public void updateTimeRange() {
        updateOutdatedTimeRange();
        setDateText(mBeginText, mBeginDate);
        setDateText(mEndText, mEndDate);
    }

    private void initTimeRange() {
        mEndDate = DateTime.now().plusDays(1).withTimeAtStartOfDay().minusSeconds(1);
        mBeginDate = mEndDate.minusDays(Config.DEFAULT_DAYS_BACK_FROM_TODAY + 1).plusSeconds(1);
    }

    private void updateOutdatedTimeRange() {
        Activity activity = mFragment.getActivity();
        if (activity != null) {
            long lastUpdatedTime = PrefUtils.getLastDateRangeUpdateTime(activity);
            if (lastUpdatedTime > 0) {
                DateTime startOfLastDate = new DateTime(lastUpdatedTime).withTimeAtStartOfDay();
                DateTime startOfToday = DateTime.now().withTimeAtStartOfDay();
                if (!startOfLastDate.isEqual(startOfToday)) {
                    // It assumes the user won't bother the outdated time range setting.
                    initTimeRange();
                }
            }
        }
    }

    private void setDateText(TextView textView, DateTime datetime) {
        SpannableString text = new SpannableString(String.format("%02d/%02d/%02d",
                datetime.getYear() - 2000, datetime.getMonthOfYear(), datetime.getDayOfMonth()));
        text.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), 0);
        textView.setText(text);
    }

    private DatePickerDialog.OnDateSetListener mBeginDateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
        // monthOfYear is within [0, 11], but DateTime expects month to be [1, 12]
        mBeginDate = new DateTime(year, monthOfYear + 1, dayOfMonth, 0, 0);
        setDateText(mBeginText, mBeginDate);
        mFragment.reloadTracks(mFragment.getLoaderManager(), mBeginDate.getMillis(), mEndDate.getMillis(), mFragment);
        PrefUtils.setLastDateRangeUpdateTime(mFragment.getActivity());
    };

    private DatePickerDialog.OnDateSetListener mEndDateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
        // monthOfYear is within [0, 11], but DateTime expects month to be [1, 12]
        mEndDate = new DateTime(year, monthOfYear + 1, dayOfMonth, 0, 0);
        setDateText(mEndText, mEndDate);
        mFragment.reloadTracks(mFragment.getLoaderManager(), mBeginDate.getMillis(), mEndDate.getMillis(), mFragment);
        PrefUtils.setLastDateRangeUpdateTime(mFragment.getActivity());
    };
}
