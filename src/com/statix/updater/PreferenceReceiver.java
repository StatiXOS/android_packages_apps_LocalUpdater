package com.statix.updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.statix.updater.misc.Utilities;

public class PreferenceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Utilities.resetPreferences(context);
        Utilities.cleanInternalDir();
        Utilities.cleanUpdateDir(context);
    }
}
