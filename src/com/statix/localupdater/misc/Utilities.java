package com.statix.localupdater.misc;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.statix.localupdater.model.ABUpdate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

public class Utilities {

    public static File[] lsFiles(File dir) {
        return dir.getAbsoluteFile().listFiles();
    }

    public static boolean isUpdate(File update) {
        String updateName = update.getName();
        // current build properties
        String currentBuild = SystemProperties.get(Constants.STATIX_VERSION_PROP);
        String buildPrefix = SystemProperties.get(Constants.DEVICE_PROP);
        double version = Double.parseDouble(currentBuild.substring(1, 4));
        String variant = SystemProperties.get(Constants.STATIX_BUILD_TYPE_PROP);
        // upgrade build properties
        String[] split = updateName.split("-");
        String upgradePrefix = split[0];
        double upgradeVersion = Double.parseDouble(split[4].substring(1));
        String upgradeVariant = split[5].split("\\.")[0];
        boolean prefixes = buildPrefix.equals(upgradePrefix);
        boolean versionUpgrade = upgradeVersion >= version;
        boolean sameVariant = upgradeVariant.equals(variant);
        return prefixes && versionUpgrade && sameVariant;
    }

    public static void copyUpdate(ABUpdate source) {
        File src = source.update();
        String name = src.getName();
        int pos = name.lastIndexOf(".");
        if (pos > 0) {
            name = name.substring(0, pos);
        }
        try {
            File dest = new File(Constants.UPDATE_INTERNAL_DIR + name);
            copy(new FileInputStream(src), new FileOutputStream(dest));
            source.setUpdate(dest);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Utilities", "Unable to copy update");
        }
    }

    private static void copy(FileInputStream is, FileOutputStream os) throws IOException {
        try {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    public static ABUpdate checkForUpdates(Context context) {
        File[] updates = lsFiles(context.getExternalFilesDir(null));
        Collections.sort(Arrays.asList(updates));
        Collections.reverse(Arrays.asList(updates));
        if (updates != null) {
            for (File update : updates) {
                if (isUpdate(update)) {
                    return new ABUpdate(update);
                }
            }
        }
        return null;
    }

    public static String[] getPayloadProperties(File update) {
        String[] headerKeyValuePairs;
        try {
            ZipFile zipFile = new ZipFile(update);
            ZipEntry payloadPropEntry = zipFile.getEntry("payload_properties.txt");
            try (InputStream is = zipFile.getInputStream(payloadPropEntry);
                 InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader br = new BufferedReader(isr)) {
                 List<String> lines = new ArrayList<>();
                 for (String line; (line = br.readLine()) != null;) {
                     lines.add(line);
                 }
                 headerKeyValuePairs = new String[lines.size()];
                 headerKeyValuePairs = lines.toArray(headerKeyValuePairs);
            }
            zipFile.close();
            return headerKeyValuePairs;
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    public static long getZipOffset(String zipFilePath) throws IOException {
        ZipFile zipFile = new ZipFile(zipFilePath);
        // Each entry has an header of (30 + n + m) bytes
        // 'n' is the length of the file name
        // 'm' is the length of the extra field
        final int FIXED_HEADER_SIZE = 30;
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        long offset = 0;
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            int n = entry.getName().length();
            int m = entry.getExtra() == null ? 0 : entry.getExtra().length;
            int headerSize = FIXED_HEADER_SIZE + n + m;
            offset += headerSize;
            if (entry.getName().equals("payload.bin")) {
                return offset;
            }
            offset += entry.getCompressedSize();
        }
        Log.e("ABUpdater", "payload.bin not found");
        throw new IllegalArgumentException("The given entry was not found");
    }

    public static void cleanUpdateDir(Context context) {
        File[] updateDirPush = lsFiles(context.getExternalFilesDir(null));
        if (updateDirPush != null) {
            for (File f : updateDirPush) {
                f.delete();
            }
        }
    }

    public static void cleanInternalDir() {
        File[] updateDirInternal = lsFiles(new File(Constants.UPDATE_INTERNAL_DIR));
        if (updateDirInternal != null) {
            for (File f : updateDirInternal) {
                if (!f.getName().equals(Constants.HISTORY_FILE)) {
                    f.delete();
                }
            }
        }
    }

    public static void putPref(String preference, boolean newValue, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(preference, newValue)
                .apply();
    }

    public static void resetPreferences(Context context) {
        for (String pref : Constants.PREFS_LIST) {
            putPref(pref, false, context);
        }
    }
}
