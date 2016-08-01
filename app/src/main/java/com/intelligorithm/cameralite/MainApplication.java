package com.intelligorithm.cameralite;

import android.app.Application;
import android.content.Context;

/**
 * Created by rescobar on 01/08/2016.
 */
public class MainApplication extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();

    }

    public static Context getAppContext() {
        return MainApplication.context;
    }
}
