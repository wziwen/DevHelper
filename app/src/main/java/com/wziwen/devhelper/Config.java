package com.wziwen.devhelper;

import com.wziwen.devhelper.base.App;

import java.io.File;

/**
 * Created by wen on 2016/9/25.
 */

public class Config {

    private static String CACHE_DIR;

    public static String getDownloadDir() {
        if (CACHE_DIR == null) {
            CACHE_DIR = App.getInstance().getFilesDir().getAbsolutePath() + File.separator;
        }
        return CACHE_DIR;
    }
}
