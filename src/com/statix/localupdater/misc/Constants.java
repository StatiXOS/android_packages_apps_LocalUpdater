package com.statix.localupdater.misc;

public class Constants {

    // Data constants
    public static final String UPDATE_INTERNAL_DIR = "/data/statix_updates/";

    // Update constants
    public static String DEVICE_PROP = "ro.statix.device";
    public static String STATIX_VERSION_PROP = "ro.statix.version";
    public static String STATIX_BUILD_TYPE_PROP = "ro.statix.buildtype";

    // Status constants
    public static final int UPDATE_FINALIZING = 0;
    public static final int UPDATE_STOPPED = 1;
    public static final int UPDATE_PAUSED = 2;
    public static final int UPDATE_FAILED = 3;
    public static final int UPDATE_SUCCEEDED = 4;
    public static final int UPDATE_IN_PROGRESS = 5;
    public static final int UPDATE_VERIFYING = 6;

    // Preference Constants
    public static final String PREF_INSTALLING_SUSPENDED_AB = "installation_suspended_ab";
    public static final String PREF_INSTALLING_AB = "installing_ab";
    public static final String PREF_INSTALLED_AB = "installed_ab";
    public static final String ENABLE_AB_PERF_MODE = "ab_perf_mode";
    public static final String[] PREFS_LIST = new String[]{PREF_INSTALLING_SUSPENDED_AB, PREF_INSTALLED_AB, PREF_INSTALLING_AB};

    // History constants
    public static final String HISTORY_FILE = "history.json";
    public static final String HISTORY_PATH = UPDATE_INTERNAL_DIR + HISTORY_FILE;
}
