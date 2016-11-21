package com.wziwen.devhelper.base;

import android.support.multidex.MultiDexApplication;

/**
 * Created by wen on 2016/9/25.
 */
public class App extends MultiDexApplication {

    static App instance;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
