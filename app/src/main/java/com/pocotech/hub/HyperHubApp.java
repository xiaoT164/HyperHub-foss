package com.pocotech.hub;

import android.app.Application;
import android.content.Context;

public class HyperHubApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }
}
