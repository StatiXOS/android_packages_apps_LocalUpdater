package com.statix.localupdater.model;

public class HistoryCard implements Comparable {

    private String mName;
    private boolean mSuccessful;

    public HistoryCard(String updateName, boolean success) {
        mName = updateName;
        mSuccessful = success;
    }

    public String getUpdateName() {
        return mName;
    }

    public boolean updateSucceeded() {
        return mSuccessful;
    }

    @Override
    public int compareTo(Object o) {
        HistoryCard other = (HistoryCard) o;
        return mName.compareTo(other.getUpdateName());
    }
}
