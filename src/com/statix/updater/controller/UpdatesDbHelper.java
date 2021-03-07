package com.statix.updater.controller;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.statix.updater.model.Update;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UpdatesDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "updates.db";

    public static class UpdateEntry implements BaseColumns {
        public static final String TABLE_NAME = "updates";
        public static final String COLUMN_NAME_STATUS = "status";
        public static final String COLUMN_NAME_PATH = "path";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME = "name";
    }

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + UpdateEntry.TABLE_NAME + " (" +
                    UpdateEntry.COLUMN_NAME + "TEXT PRIMARY KEY," +
                    UpdateEntry.COLUMN_NAME_STATUS + " INTEGER," +
                    UpdateEntry.COLUMN_NAME_PATH + " TEXT," +
                    UpdateEntry.COLUMN_NAME_TIMESTAMP + " INTEGER)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + UpdateEntry.TABLE_NAME;

    public UpdatesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public long addUpdate(Update update) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        fillContentValues(update, values);
        return db.insert(UpdateEntry.TABLE_NAME, null, values);
    }

    public long addUpdateWithOnConflict(Update update, int conflictAlgorithm) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        fillContentValues(update, values);
        return db.insertWithOnConflict(UpdateEntry.TABLE_NAME, null, values, conflictAlgorithm);
    }

    private static void fillContentValues(Update update, ContentValues values) {
        values.put(UpdateEntry.COLUMN_NAME, update.getName());
        values.put(UpdateEntry.COLUMN_NAME_STATUS, update.state());
        values.put(UpdateEntry.COLUMN_NAME_PATH, update.getUpdatePath());
    }

    public boolean removeUpdate(String updateName) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = UpdateEntry.COLUMN_NAME + " = ?";
        String[] selectionArgs = {updateName};
        return db.delete(UpdateEntry.TABLE_NAME, selection, selectionArgs) != 0;
    }

    public boolean changeUpdateStatus(Update update) {
        String selection = UpdateEntry.COLUMN_NAME + " = ?";
        String[] selectionArgs = {update.update().getName()};
        return changeUpdateStatus(selection, selectionArgs, update.state());
    }

    private boolean changeUpdateStatus(String selection, String[] selectionArgs,
                                       int status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UpdateEntry.COLUMN_NAME_STATUS, status);
        return db.update(UpdateEntry.TABLE_NAME, values, selection, selectionArgs) != 0;
    }

    public Update getUpdate(String updateName) {
        String selection = UpdateEntry.COLUMN_NAME + " = ?";
        String[] selectionArgs = {updateName};
        return getUpdate(selection, selectionArgs);
    }

    private Update getUpdate(String selection, String[] selectionArgs) {
        List<Update> updates = getUpdates(selection, selectionArgs);
        return updates != null ? updates.get(0) : null;
    }

    public List<Update> getUpdates() {
        return getUpdates(null, null);
    }

    public List<Update> getUpdates(String selection, String[] selectionArgs) {
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {
                UpdateEntry.COLUMN_NAME_PATH,
                UpdateEntry.COLUMN_NAME_STATUS,
        };
        String sort = UpdateEntry.COLUMN_NAME_TIMESTAMP + " DESC";
        Cursor cursor = db.query(UpdateEntry.TABLE_NAME, projection, selection, selectionArgs,
                null, null, sort);
        List<Update> updates = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Update update = new Update();
                int index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_PATH);
                update.setUpdate(new File(cursor.getString(index)));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_STATUS);
                update.setState(cursor.getInt(index));
                updates.add(update);
            }
            cursor.close();
        }
        return updates;
    }
}