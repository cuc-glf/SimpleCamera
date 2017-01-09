package com.example.gaolf.simplecamera;

import android.app.Application;

/**
 * Created by gaolf on 17/1/9.
 */

public class MyApplication extends Application {

    private static Application sApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
    }

    public static Application getApplication() {
        return sApplication;
    }
}
