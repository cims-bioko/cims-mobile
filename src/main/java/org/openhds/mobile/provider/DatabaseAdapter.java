package org.openhds.mobile.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.openhds.mobile.model.core.Supervisor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DatabaseAdapter {

    private static final String DATABASE_NAME = "entityData";
    private static final int DATABASE_VERSION = 16;

    private static final String SUPERVISOR_TABLE_NAME = "openhds_supervisor";
    private static final String KEY_ID = "_id";
    private static final String KEY_SUPERVISOR_NAME = "username";
    private static final String KEY_SUPERVISOR_PASS = "password";

    private static final String FORM_PATH_TABLE_NAME = "path_to_forms";
    private static final String FORM_PATH_IDX_NAME = "path_id";
    private static final String KEY_HIER_PATH = "hierarchyPath";
    private static final String KEY_FORM_PATH = "formPath";

    private static final String USER_CREATE = "CREATE TABLE " + SUPERVISOR_TABLE_NAME + " ("
            + KEY_ID + " INTEGER PRIMARY KEY, "
            + KEY_SUPERVISOR_NAME + " TEXT, "
            + KEY_SUPERVISOR_PASS + " TEXT)";

    private static final String FORM_PATH_CREATE = "CREATE TABLE " + FORM_PATH_TABLE_NAME + " ("
            + KEY_HIER_PATH + " TEXT, " + KEY_FORM_PATH + " TEXT, CONSTRAINT "
            + FORM_PATH_IDX_NAME + " UNIQUE (" + KEY_HIER_PATH + ", " + KEY_FORM_PATH + " ) )";

    private static DatabaseAdapter instance;

    public static synchronized DatabaseAdapter getInstance(Context ctx) {
        if (instance == null) {
            instance = new DatabaseAdapter(ctx);
        }
        return instance;
    }

    private DatabaseHelper helper;

    protected DatabaseAdapter(Context context) {
        helper = new DatabaseHelper(context);
    }

    public Supervisor findSupervisorByUsername(String username) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String[] columns = {KEY_ID, KEY_SUPERVISOR_NAME, KEY_SUPERVISOR_PASS};
        String where = String.format("%s = ?", KEY_SUPERVISOR_NAME);
        String[] whereArgs = {username};
        Cursor cursor = db.query(SUPERVISOR_TABLE_NAME, columns, where, whereArgs, null, null, null);
        Supervisor user = null;
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    user = new Supervisor();
                    user.setId(cursor.getLong(cursor.getColumnIndex(KEY_ID)));
                    user.setName(cursor.getString(cursor.getColumnIndex(KEY_SUPERVISOR_NAME)));
                    user.setPassword(cursor.getString(cursor.getColumnIndex(KEY_SUPERVISOR_PASS)));
                }
            } finally {
                cursor.close();
            }
        }
        return user;
    }

    public long addSupervisor(Supervisor u) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(KEY_SUPERVISOR_NAME, u.getName());
            cv.put(KEY_SUPERVISOR_PASS, u.getPassword());
            long id = db.insert(SUPERVISOR_TABLE_NAME, null, cv);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public int deleteSupervisor(Supervisor u) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int rowCount = -1;
        db.beginTransaction();
        try {
            String where = String.format("%s = ?", KEY_SUPERVISOR_NAME);
            String[] whereArgs = {u.getName()};
            rowCount = db.delete(SUPERVISOR_TABLE_NAME, where, whereArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return rowCount;
    }

    public long createAssociation(String hierarchyPath, String filePath) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(KEY_HIER_PATH, hierarchyPath);
            cv.put(KEY_FORM_PATH, filePath);
            long id = db.replaceOrThrow(FORM_PATH_TABLE_NAME, null, cv);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public Collection<String> findAssociatedPath(String hierarchyPath) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Set<String> formPaths = new HashSet<>();
        String[] columns = {KEY_FORM_PATH};
        String where = String.format("%s = ?", KEY_HIER_PATH);
        String[] whereArgs = {hierarchyPath};
        Cursor cursor = db.query(FORM_PATH_TABLE_NAME, columns, where, whereArgs, null, null, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    formPaths.add(cursor.getString(cursor.getColumnIndex(KEY_FORM_PATH)));
                }
            } finally {
                cursor.close();
            }
        }
        return formPaths;
    }

    public Collection<String> findAllAssociatedPaths() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Set<String> associatedPaths = new HashSet<>();
        String[] columns = {KEY_FORM_PATH};
        Cursor cursor = db.query(FORM_PATH_TABLE_NAME, columns, null, null, null, null, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    associatedPaths.add(cursor.getString(cursor.getColumnIndex(KEY_FORM_PATH)));
                }
            } finally {
                cursor.close();
            }
        }
        return associatedPaths;
    }

    public void deleteAssociatedPath(String sentFilepath) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            String where = String.format("%s = ?", KEY_FORM_PATH);
            String[] whereArgs = {sentFilepath};
            db.delete(FORM_PATH_TABLE_NAME, where, whereArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(USER_CREATE);
            db.execSQL(FORM_PATH_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onCreate(db);
        }
    }
}
