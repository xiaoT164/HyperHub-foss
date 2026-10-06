package com.pocotech.hub;

import android.app.Application;
import android.content.Context;

public class HyperHubApp extends Application {

    /**
     * Локаль применяется один раз и безопасно.
     * В attachBaseContext() нельзя брать getApplicationContext() — он ещё null,
     * поэтому LocaleHelper.wrap() вызывается из onCreate(), где контекст уже готов.
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LocaleHelper.apply(this);
    }
}
