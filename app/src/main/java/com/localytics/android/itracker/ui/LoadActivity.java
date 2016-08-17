package com.localytics.android.itracker.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.localytics.android.itracker.Application;
import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.ActivityManager;
import com.localytics.android.itracker.data.LogManager;
import com.localytics.android.itracker.data.account.OnAccountChangedListener;
import com.localytics.android.itracker.service.ImService;
import com.localytics.android.itracker.ui.helper.SingleActivity;

import java.util.Collection;

public class LoadActivity extends SingleActivity implements OnAccountChangedListener {

    private Animation mAnimation;
    private View mDisconnectedView;

    public static Intent createIntent(Context context) {
        return new Intent(context, LoadActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);
        mAnimation = AnimationUtils.loadAnimation(this, R.anim.connection);
        mDisconnectedView = findViewById(R.id.disconnected);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        if (Application.getInstance().isClosing()) {
            ((TextView) findViewById(R.id.text)).setText(R.string.application_state_closing);
        } else {
            startService(ImService.createIntent(this));
            mDisconnectedView.startAnimation(mAnimation);
            update();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        mDisconnectedView.clearAnimation();
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        update();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                cancel();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void update() {
        if (Application.getInstance().isInitialized()
                && !Application.getInstance().isClosing() && !isFinishing()) {
            LogManager.i(this, "Initialized");
            finish();
        }
    }

    private void cancel() {
        finish();
        ActivityManager.getInstance().cancelTask(this);
    }

}