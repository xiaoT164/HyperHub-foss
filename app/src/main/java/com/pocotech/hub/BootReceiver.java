package com.pocotech.hub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Восстанавливает ежедневное напоминание после перезагрузки устройства. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ReminderReceiver.applyFromPrefs(context);
        }
    }
}
