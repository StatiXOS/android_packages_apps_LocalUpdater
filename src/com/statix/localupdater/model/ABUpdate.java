package com.statix.localupdater.model;

import java.io.File;
import java.io.Serializable;

/** Represents an A/B update */
public class ABUpdate implements Serializable {

    private int mState;
    private File mUpdate;
    private int mProgress;

    public ABUpdate(File update) {
        mUpdate = update;
    }

    public File update() {
        return mUpdate;
    }

    public int getProgress() {
        return mProgress;
    }

    public int state() {
        return mState;
    }

    public void setState(int state) {
        mState = state;
    }

    public void setProgress(int progress) {
        mProgress = progress;
    }

    public void setUpdate(File update) {
        mUpdate = update;
    }

    public String getUpdatePath() {
        return mUpdate.getAbsolutePath();
    }
}
