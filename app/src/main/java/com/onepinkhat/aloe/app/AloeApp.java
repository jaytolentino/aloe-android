package com.onepinkhat.aloe.app;

import android.app.Application;
import android.content.Context;

/**
 * Represents the Aloe application
 *
 * Created by jay on 5/22/16.
 */
public class AloeApp extends Application {

    private static Context context;

    public static Context getAppContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }
}
