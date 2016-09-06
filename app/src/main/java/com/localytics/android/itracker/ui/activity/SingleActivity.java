package com.localytics.android.itracker.ui.activity;

import android.app.Activity;
import android.os.Bundle;

import com.localytics.android.itracker.ui.activity.BaseActivity;

import java.util.HashMap;
import java.util.Map;

public abstract class SingleActivity extends BaseActivity {

    private static Map<Class<? extends Activity>, Activity> sLaunched = new HashMap<Class<? extends Activity>, Activity>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Activity activity = sLaunched.get(this.getClass());
        if (activity != null)
            activity.finish();
        sLaunched.put(this.getClass(), this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        Activity activity = sLaunched.get(this.getClass());
        if (activity == this)
            sLaunched.remove(this.getClass());
        super.onDestroy();
    }

}