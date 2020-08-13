package com.statix.updater;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import com.statix.updater.misc.Constants;
import com.statix.updater.misc.Utilities;
import com.statix.updater.model.ABUpdate;

class ABUpdateHandler {

    private boolean mBound;
    private ABUpdate mUpdate;
    private Context mContext;
    private MainViewController mController;
    private UpdateEngine mUpdateEngine;

    private final PowerManager.WakeLock mWakeLock;

    private static ABUpdateHandler sInstance = null;

    private static final String TAG = "ABUpdateHandler";

    private ABUpdateHandler(ABUpdate update, Context ctx, MainViewController controller) {
        mContext = ctx;
        mController = controller;
        mUpdate = update;
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        mUpdateEngine = new UpdateEngine();
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Updater");
        mWakeLock.setReferenceCounted(false);
    }

    public static synchronized ABUpdateHandler getInstance(ABUpdate update, Context context,
                                                           MainViewController controller) {
        if (sInstance == null) {
            sInstance = new ABUpdateHandler(update, context, controller);
        }
        return sInstance;
    }

    synchronized void handleUpdate() {
        if (!mBound) {
            mBound = mUpdateEngine.bind(mUpdateEngineCallback);
        }
        AsyncTask.execute(() -> {
            try {
                mWakeLock.acquire();
                Utilities.copyUpdate(mUpdate);
                Log.d(TAG, mUpdate.update().toString());
                String[] payloadProperties = Utilities.getPayloadProperties(mUpdate.update());
                long offset = Utilities.getZipOffset(mUpdate.getUpdatePath());
                String zipFileUri = "file://" + mUpdate.getUpdatePath();
                mUpdate.setState(Constants.UPDATE_IN_PROGRESS);
                mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_IN_PROGRESS);
                Log.d(TAG, "Applying payload");
                Utilities.putPref(Constants.PREF_INSTALLING_AB, true, mContext);
                mUpdateEngine.applyPayload(zipFileUri, offset, 0, payloadProperties);
            } catch (IOException e) {
                mWakeLock.release();
                e.printStackTrace();
                Log.e(TAG, "Unable to extract update.");
                mUpdate.setState(Constants.UPDATE_FAILED);
                mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_FAILED);
            }
        });
    }

    public void reconnect() {
        if (!mBound) {
            mBound = mUpdateEngine.bind(mUpdateEngineCallback);
            Log.d(TAG, "Reconnected to update engine");
        }
    }

    public void suspend() {
        mUpdateEngine.suspend();
        mWakeLock.release();
        Utilities.putPref(Constants.PREF_INSTALLING_SUSPENDED_AB, true, mContext);
        Utilities.putPref(Constants.PREF_INSTALLING_AB, false, mContext);
        mUpdate.setState(Constants.UPDATE_PAUSED);
    }

    public void resume() {
        mUpdateEngine.resume();
        mWakeLock.acquire();
        Utilities.putPref(Constants.PREF_INSTALLING_AB, true, mContext);
        Utilities.putPref(Constants.PREF_INSTALLING_SUSPENDED_AB, false, mContext);
        mUpdate.setState(Constants.UPDATE_IN_PROGRESS);
    }

    public void cancel() {
        Utilities.putPref(Constants.PREF_INSTALLED_AB, false, mContext);
        Utilities.putPref(Constants.PREF_INSTALLING_SUSPENDED_AB, false, mContext);
        Utilities.putPref(Constants.PREF_INSTALLING_AB, false, mContext);
        mUpdateEngine.cancel();
        mWakeLock.release();
        mUpdate.setState(Constants.UPDATE_STOPPED);
    }

    public void unbind() {
        mBound = !mUpdateEngine.unbind();
        Log.d(TAG, "Unbound callback from update engine");
    }

    private final UpdateEngineCallback mUpdateEngineCallback = new UpdateEngineCallback() {
        @Override
        public void onStatusUpdate(int status, float percent) {
            switch (status) {
                case UpdateEngine.UpdateStatusConstants.REPORTING_ERROR_EVENT:
                    mUpdate.setState(Constants.UPDATE_FAILED);
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_FAILED);
                    mWakeLock.release();
                case UpdateEngine.UpdateStatusConstants.DOWNLOADING:
                    mUpdate.setProgress(Math.round(percent * 100));
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_IN_PROGRESS);
                break;
                case UpdateEngine.UpdateStatusConstants.FINALIZING: {
                    mUpdate.setProgress(Math.round(percent * 100));
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_FINALIZING);
                }
                break;
                case UpdateEngine.UpdateStatusConstants.VERIFYING:
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_VERIFYING);
                    mUpdate.setState(Constants.UPDATE_VERIFYING);
                break;
                case UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT: {
                    mUpdate.setState(Constants.UPDATE_SUCCEEDED);
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_SUCCEEDED);
                    mWakeLock.release();
                }
                break;
                case UpdateEngine.UpdateStatusConstants.IDLE:
                    Utilities.cleanInternalDir();
            }
        }

        @Override
        public void onPayloadApplicationComplete(int errorCode) {
            mWakeLock.release();
            if (errorCode != UpdateEngine.ErrorCodeConstants.SUCCESS) {
                mUpdate.setProgress(0);
                mUpdate.setState(Constants.UPDATE_FAILED);
                Utilities.putPref(Constants.PREF_INSTALLED_AB, false, mContext);
                Utilities.putPref(Constants.PREF_INSTALLING_SUSPENDED_AB, false, mContext);
                Utilities.putPref(Constants.PREF_INSTALLING_AB, false, mContext);
                mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_FAILED);
            }
        }
    };

    public void setPerformanceMode(boolean checked) {
        mUpdateEngine.setPerformanceMode(checked);
        Utilities.putPref(Constants.ENABLE_AB_PERF_MODE, checked, mContext);
    }
}
