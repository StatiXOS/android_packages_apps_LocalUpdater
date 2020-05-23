package com.statix.updater;

import static com.statix.updater.misc.Constants.PREF_INSTALLED_AB;
import static com.statix.updater.misc.Constants.PREF_INSTALLING_AB;
import static com.statix.updater.misc.Constants.PREF_INSTALLING_SUSPENDED_AB;
import static com.statix.updater.misc.Constants.ENABLE_AB_PERF_MODE;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.statix.updater.history.HistoryUtils;
import com.statix.updater.history.HistoryView;
import com.statix.updater.misc.Constants;
import com.statix.updater.misc.Utilities;
import com.statix.updater.model.ABUpdate;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements MainViewController.StatusListener {

    private ABUpdateHandler mUpdateHandler;
    private ABUpdate mUpdate;
    private Button mUpdateControl;
    private Button mPauseResume;
    private ImageButton mHistory;
    private MainViewController mController;
    private ProgressBar mUpdateProgress;
    private SharedPreferences mSharedPrefs;
    private Switch mABPerfMode;
    private TextView mCurrentVersionView;
    private TextView mUpdateProgressText;
    private TextView mUpdateView;
    private TextView mUpdateSize;

    private final String TAG = "Updater";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mController = MainViewController.getInstance(getApplicationContext());
        // set up views
        mUpdateProgress = (ProgressBar) findViewById(R.id.progress_bar);
        mUpdateProgress.setVisibility(View.INVISIBLE);
        mUpdateView = (TextView) findViewById(R.id.update_view);
        mUpdateControl = (Button) findViewById(R.id.update_control);
        mPauseResume = (Button) findViewById(R.id.pause_resume);
        mHistory = (ImageButton) findViewById(R.id.history_view);
        mABPerfMode = (Switch) findViewById(R.id.perf_mode_switch);
        mCurrentVersionView = (TextView) findViewById(R.id.current_version_view);
        mUpdateProgressText = (TextView) findViewById(R.id.progressText);
        mUpdateSize = (TextView) findViewById(R.id.update_size);
        mCurrentVersionView.setText(getString(R.string.current_version, SystemProperties.get(Constants.STATIX_VERSION_PROP)));
        mHistory.setOnClickListener(v -> {
            Log.d(TAG, "History imagebutton clicked");
            Intent histIntent = new Intent(getApplicationContext(), HistoryView.class);
            startActivity(histIntent);
        });

        // set up prefs
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // check for updoots in /sdcard/statix_updates
        mUpdate = Utilities.checkForUpdates(getApplicationContext());

        mUpdateControl.setOnClickListener(v -> {
            String buttonText = mUpdateControl.getText().toString();
            String cancel = getString(R.string.cancel_update);
            String check = getString(R.string.check_for_update);
            String apply = getString(R.string.apply_update);
            if (buttonText.equals(cancel)) {
                mUpdateHandler.cancel();
                Log.d(TAG, "Update cancelled");
                mUpdateProgress.setVisibility(View.INVISIBLE);
                mUpdateProgressText.setVisibility(View.INVISIBLE);
                mPauseResume.setVisibility(View.INVISIBLE);
                mUpdateControl.setText(R.string.reboot_device);
            } else if (buttonText.equals(check)) {
                mUpdate = Utilities.checkForUpdates(getApplicationContext());
                setUpView();
            } else if (buttonText.equals(apply)){
                mUpdateHandler.handleUpdate();
                mUpdateControl.setText(R.string.cancel_update);
                mPauseResume.setVisibility(View.VISIBLE);
                mPauseResume.setText(R.string.pause_update);
            } else { // reboot
                showRebootDialog();
            }
        });

        setUpView();
    }

    private void setUpView() {
        if (mUpdate != null) {
            mUpdateHandler = ABUpdateHandler.getInstance(mUpdate, getApplicationContext(), mController);
            mController.addUpdateStatusListener(this);
            if (mSharedPrefs.getBoolean(PREF_INSTALLING_SUSPENDED_AB, false)
                    || mSharedPrefs.getBoolean(PREF_INSTALLING_AB, false)
                    || mSharedPrefs.getBoolean(PREF_INSTALLED_AB, false)) {
                mUpdateHandler.reconnect();
            }
            // ab perf switch
            mABPerfMode.setVisibility(View.VISIBLE);
            mABPerfMode.setChecked(mSharedPrefs.getBoolean(ENABLE_AB_PERF_MODE, false));
            mABPerfMode.setOnClickListener(v -> {
                mUpdateHandler.setPerformanceMode(mABPerfMode.isChecked());
                Log.d(TAG, Boolean.toString(mSharedPrefs.getBoolean(ENABLE_AB_PERF_MODE, false)));
            });
            mUpdateHandler.setPerformanceMode(mABPerfMode.isChecked());
            // apply updoot button
            String updateText = getString(R.string.to_install, mUpdate.update().getName());
            mUpdateView.setText(updateText);
            String updateSizeMB = getString(R.string.update_size, Long.toString(mUpdate.update().length()/(1024*1024)));
            mUpdateSize.setText(updateSizeMB);
            mUpdateControl.setText(R.string.apply_update);
            // pause/resume
            mPauseResume.setVisibility(View.INVISIBLE);
            mPauseResume.setOnClickListener(v -> {
                boolean updatePaused = mSharedPrefs.getBoolean(PREF_INSTALLING_SUSPENDED_AB, false);
                if (updatePaused) {
                    mPauseResume.setText(R.string.pause_update);
                    mUpdateHandler.resume();
                    mUpdateProgress.setVisibility(View.VISIBLE);
                } else {
                    mPauseResume.setText(R.string.resume_update);
                    mUpdateHandler.suspend();
                }
            });
            setButtonVisibilities();
        } else {
            mUpdateView.setText(R.string.no_update_available);
            mUpdateControl.setText(R.string.check_for_update);
            mPauseResume.setVisibility(View.INVISIBLE);
            mABPerfMode.setVisibility(View.INVISIBLE);
        }
    }

    private void setButtonVisibilities() {
        if (mSharedPrefs.getBoolean(PREF_INSTALLING_SUSPENDED_AB, false)) {
            mPauseResume.setVisibility(View.VISIBLE);
            mPauseResume.setText(R.string.resume_update);
            mUpdateControl.setText(R.string.cancel_update);
        } else if (mSharedPrefs.getBoolean(PREF_INSTALLED_AB, false)) {
            mPauseResume.setVisibility(View.INVISIBLE);
            mUpdateControl.setText(R.string.reboot_device);
            mUpdateProgressText.setText(R.string.update_complete);
        } else if (mSharedPrefs.getBoolean(PREF_INSTALLING_AB, false)) {
            mPauseResume.setVisibility(View.VISIBLE);
            mPauseResume.setText(R.string.pause_update);
            mUpdateControl.setText(R.string.cancel_update);
            mUpdateProgress.setVisibility(View.VISIBLE);
        }
        mABPerfMode.setChecked(mSharedPrefs.getBoolean(ENABLE_AB_PERF_MODE, false));
    }

    @Override
    public void onUpdateStatusChanged(ABUpdate update, int state) {
        int updateProgress = update.getProgress();
        File f = new File(Constants.HISTORY_PATH);
        mUpdate.setState(state);
        runOnUiThread(() -> {
            switch (state) {
                case Constants.UPDATE_FAILED:
                    update.setProgress(0);
                    mUpdateProgress.setVisibility(View.INVISIBLE);
                    mUpdateProgressText.setText(R.string.reboot_try_again);
                    mUpdateControl.setText(R.string.reboot_device);
                    mPauseResume.setVisibility(View.INVISIBLE);
                    try {
                        HistoryUtils.writeUpdateToJson(f, mUpdate);
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Unable to write to update history.");
                    }
                    Utilities.cleanInternalDir();
                    break;
                case Constants.UPDATE_FINALIZING:
                    mUpdateProgress.setProgress(updateProgress);
                    mUpdateProgressText.setText(getString(R.string.update_finalizing, Integer.toString(updateProgress)));
                    break;
                case Constants.UPDATE_IN_PROGRESS:
                    mPauseResume.setVisibility(View.VISIBLE);
                    mPauseResume.setText(R.string.pause_update);
                    mUpdateControl.setText(R.string.cancel_update);
                    mUpdateProgressText.setVisibility(View.VISIBLE);
                    mUpdateProgressText.setText(getString(R.string.installing_update, Integer.toString(updateProgress)));
                    mUpdateProgress.setVisibility(View.VISIBLE);
                    mUpdateProgress.setProgress(updateProgress);
                    break;
                case Constants.UPDATE_VERIFYING:
                    mPauseResume.setVisibility(View.INVISIBLE);
                    mUpdateView.setText(R.string.verifying_update);
                case Constants.UPDATE_SUCCEEDED:
                    Utilities.cleanUpdateDir(getApplicationContext());
                    Utilities.cleanInternalDir();
                    try {
                        HistoryUtils.writeUpdateToJson(f, mUpdate);
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Unable to write to update history.");
                    }
                    Utilities.putPref(Constants.PREF_INSTALLED_AB, true, getApplicationContext());
                    mPauseResume.setVisibility(View.INVISIBLE);
                    mUpdateProgress.setVisibility(View.INVISIBLE);
                    mUpdateProgressText.setText(R.string.update_complete);
                    mUpdateControl.setText(R.string.reboot_device);
                    break;
            }
        });
    }

    @Override
    public void onStop() {
        mController.removeUpdateStatusListener(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ABUpdate update = Utilities.checkForUpdates(getApplicationContext());
        if (update != null && !update.equals(mUpdate)) {
            mUpdate = update;
        }
        if (mController != null) {
            mController.addUpdateStatusListener(this);
        }
        if (mUpdateHandler != null) {
            mUpdateHandler.reconnect();
            Log.d(TAG, "Reconnected to update engine");
        }
        setButtonVisibilities();
    }

    @Override
    protected void onPause() {
        if (mController != null) {
            mController.removeUpdateStatusListener(this);
        }
        if (mUpdateHandler != null) {
            mUpdateHandler.unbind();
            Log.d(TAG, "Unbound callback from update engine");
        }
        super.onPause();
    }

    private void rebootDevice() {
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        Utilities.resetPreferences(getApplicationContext());
        pm.reboot("Update complete");
    }

    private void showRebootDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.restart_title)
                .setMessage(R.string.reboot_message)
                .setPositiveButton(R.string.ok, (dialog, id) -> rebootDevice())
                .setNegativeButton(R.string.cancel, null).show();
    }
}
