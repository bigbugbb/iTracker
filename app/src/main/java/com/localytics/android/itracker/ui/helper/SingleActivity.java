package com.localytics.android.itracker.ui.helper;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;

import com.localytics.android.itracker.ui.ManagedActivity;

public abstract class SingleActivity extends ManagedActivity {

    private static Map<Class<? extends Activity>, Activity> launched = new HashMap<Class<? extends Activity>, Activity>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Activity activity = launched.get(this.getClass());
        if (activity != null)
            activity.finish();
        launched.put(this.getClass(), this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        Activity activity = launched.get(this.getClass());
        if (activity == this)
            launched.remove(this.getClass());
        super.onDestroy();
    }

}