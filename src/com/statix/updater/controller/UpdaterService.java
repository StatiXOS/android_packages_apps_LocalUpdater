package com.statix.updater.controller;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.statix.updater.MainActivity;
import com.statix.updater.MainViewController;
import com.statix.updater.misc.Constants;
import com.statix.updater.model.Update;

public class UpdaterService extends Service {

    private final String TAG = "UpdaterService";
    private BroadcastReceiver mBroadcastReceiver;
    private MainViewController mViewController;
    private UpdatesDbHelper mDbHelper;

    private final IBinder mBinder = new LocalBinder();
    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;
    private NotificationCompat.BigTextStyle mNotificationStyle;

    private boolean mHasClients;
    private static final int NOTIFICATION_ID = 10;
    private static final String ONGOING_NOTIFICATION_CHANNEL =
            "ongoing_notification_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        mViewController = MainViewController.getInstance(this);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder.setContentIntent(intent);
        mDbHelper = new UpdatesDbHelper(this);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String updateName = intent.getStringExtra(Constants.INTENT_UPDATE_NAME);
                if (Constants.ACTION_UPDATE_STATUS.equals(intent.getAction())) {
                    Update update = mDbHelper.getUpdate(updateName);
                    setNotificationTitle(update);
                    Bundle extras = new Bundle();
                    extras.putString(Constants.INTENT_UPDATE_NAME, updateName);
                    mNotificationBuilder.setExtras(extras);
                    handleUpdateStatusChange(update);
                } else if (Constants.ACTION_INSTALL_PROGRESS.equals(intent.getAction())) {
                    Update update = mDbHelper.getUpdate(updateName);
                    setNotificationTitle(update);
                    handleInstallProgress(update);
                } else if (Constants.ACTION_UPDATE_COMPLETED.equals(intent.getAction())) {
                    Bundle extras = mNotificationBuilder.getExtras();
                    if (extras != null && updateName.equals(
                            extras.getString(Constants.INTENT_UPDATE_NAME))) {
                        mNotificationBuilder.setExtras(null);
                        Update update = mDbHelper.getUpdate(updateName);
                        if (update.state() != Constants.UPDATE_SUCCEEDED) {
                            mNotificationManager.cancel(NOTIFICATION_ID);
                        }
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_INSTALL_PROGRESS);
        intentFilter.addAction(Constants.ACTION_UPDATE_STATUS);
        intentFilter.addAction(Constants.ACTION_UPDATE_COMPLETED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    public class LocalBinder extends Binder {
        public UpdaterService getService() {
            return UpdaterService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mHasClients = false;
        tryStopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mHasClients = true;
        return mBinder;
    }

    private void tryStopSelf() {
        if (!mHasClients && !isInstallingUpdate()) {
            Log.d(TAG, "Service no longer needed, stopping");
            stopSelf();
        }
    }
}
