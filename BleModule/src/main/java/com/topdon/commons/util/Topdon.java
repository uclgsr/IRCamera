package com.topdon.commons.util;

import android.content.Context;

public class Topdon {
    private static Context app;

    public static void init(Context context) {
        // Use application context to prevent memory leaks
        app = context.getApplicationContext();
    }

    public static Context getApp() {
        return app;
    }
}
