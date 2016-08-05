package com.jskaleel.speedalert.splash;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;


import com.jskaleel.speedalert.MapsActivity;
import com.jskaleel.speedalert.R;
import com.jskaleel.speedalert.utils.DeviceUtils;

import java.lang.ref.WeakReference;

public class SplashActivity extends Activity {

    private static final long SLEEP_DURATION = 3000;
    private boolean isDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DeviceUtils.setStatusBarColor(this, R.color.primary_dark);
        setContentView(R.layout.activity_splash);

        HTFHandler mHandler = new HTFHandler(SplashActivity.this);
        mHandler.sendEmptyMessageDelayed(1, SLEEP_DURATION);
    }

    private class HTFHandler extends Handler {
        private WeakReference<SplashActivity> splash;

        public HTFHandler(SplashActivity splash) {
            this.splash = new WeakReference<>(splash);
        }

        @Override
        public void handleMessage(Message msg) {
            SplashActivity activity = splash.get();
            if (activity != null && msg.what == 1 && !activity.isDestroyed) {
                activity.launchNextActivity();
            }
        }
    }

    private void launchNextActivity() {
        startActivity(new Intent(this, MapsActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        super.onDestroy();
    }
}
