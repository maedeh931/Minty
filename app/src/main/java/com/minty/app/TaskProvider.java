package com.minty.app;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * TaskProvider exposes the local tasks SQLite database table via standard Android ContentProvider.
 * Authority: com.minty.app.provider
 * URIs:
 *  - content://com.minty.app.provider/tasks (Directory)
 *  - content://com.minty.app.provider/tasks/# (Single task item)
 */
public class TaskProvider extends ContentProvider {

    private static final String TAG = "TaskProvider";

    public static final String AUTHORITY = "com.minty.app.provider";
    public static final String PATH_TASKS = "tasks";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_TASKS);

    private static final int CODE_TASKS = 1;
    private static final int CODE_TASK_ID = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, PATH_TASKS, CODE_TASKS);
        uriMatcher.addURI(AUTHORITY, PATH_TASKS + "/#", CODE_TASK_ID);
    }

    private DatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate: Initializing TaskProvider with DatabaseHelper");
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int match = uriMatcher.match(uri);
        Cursor cursor;

        switch (match) {
            case CODE_TASKS:
                cursor = db.query(DatabaseHelper.TABLE_TASKS, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case CODE_TASK_ID:
                long id = ContentUris.parseId(uri);
                String idSelection = DatabaseHelper.COLUMN_TASK_ID + " = ?";
                String[] idArgs = new String[]{String.valueOf(id)};

                if (selection != null && !selection.trim().isEmpty()) {
                    idSelection = idSelection + " AND (" + selection + ")";
                    if (selectionArgs != null) {
                        String[] combinedArgs = new String[selectionArgs.length + 1];
                        combinedArgs[0] = String.valueOf(id);
                        System.arraycopy(selectionArgs, 0, combinedArgs, 1, selectionArgs.length);
                        idArgs = combinedArgs;
                    }
                }
                cursor = db.query(DatabaseHelper.TABLE_TASKS, projection, idSelection, idArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unsupported query URI: " + uri);
        }

        if (cursor != null && getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int match = uriMatcher.match(uri);
        switch (match) {
            case CODE_TASKS:
                return "vnd.android.cursor.dir/vnd.com.minty.app.provider.tasks";
            case CODE_TASK_ID:
                return "vnd.android.cursor.item/vnd.com.minty.app.provider.tasks";
            default:
                throw new IllegalArgumentException("Unsupported URI for getType: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        int match = uriMatcher.match(uri);
        if (match != CODE_TASKS) {
            throw new IllegalArgumentException("Cannot insert into URI: " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues insertValues = values != null ? new ContentValues(values) : new ContentValues();

        if (!insertValues.containsKey(DatabaseHelper.COLUMN_TASK_CREATED_AT)) {
            insertValues.put(DatabaseHelper.COLUMN_TASK_CREATED_AT, System.currentTimeMillis());
        }
        if (!insertValues.containsKey(DatabaseHelper.COLUMN_TASK_COMPLETED)) {
            insertValues.put(DatabaseHelper.COLUMN_TASK_COMPLETED, 0);
        }
        if (!insertValues.containsKey(DatabaseHelper.COLUMN_TASK_COMPLETED_AT)) {
            insertValues.put(DatabaseHelper.COLUMN_TASK_COMPLETED_AT, 0);
        }

        long rowId = db.insert(DatabaseHelper.TABLE_TASKS, null, insertValues);
        if (rowId > 0) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
            if (getContext() != null) {
                getContext().getContentResolver().notifyChange(newUri, null);
            }
            Log.d(TAG, "insert: Task inserted via ContentProvider at URI: " + newUri);
            return newUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int match = uriMatcher.match(uri);
        int rowsDeleted;

        switch (match) {
            case CODE_TASKS:
                rowsDeleted = db.delete(DatabaseHelper.TABLE_TASKS, selection, selectionArgs);
                break;
            case CODE_TASK_ID:
                long id = ContentUris.parseId(uri);
                String idSelection = DatabaseHelper.COLUMN_TASK_ID + " = ?";
                String[] idArgs = new String[]{String.valueOf(id)};

                if (selection != null && !selection.trim().isEmpty()) {
                    idSelection = idSelection + " AND (" + selection + ")";
                    if (selectionArgs != null) {
                        String[] combinedArgs = new String[selectionArgs.length + 1];
                        combinedArgs[0] = String.valueOf(id);
                        System.arraycopy(selectionArgs, 0, combinedArgs, 1, selectionArgs.length);
                        idArgs = combinedArgs;
                    }
                }
                rowsDeleted = db.delete(DatabaseHelper.TABLE_TASKS, idSelection, idArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported delete URI: " + uri);
        }

        if (rowsDeleted > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        Log.d(TAG, "delete: " + rowsDeleted + " row(s) deleted for URI: " + uri);
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int match = uriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case CODE_TASKS:
                rowsUpdated = db.update(DatabaseHelper.TABLE_TASKS, values, selection, selectionArgs);
                break;
            case CODE_TASK_ID:
                long id = ContentUris.parseId(uri);
                String idSelection = DatabaseHelper.COLUMN_TASK_ID + " = ?";
                String[] idArgs = new String[]{String.valueOf(id)};

                if (selection != null && !selection.trim().isEmpty()) {
                    idSelection = idSelection + " AND (" + selection + ")";
                    if (selectionArgs != null) {
                        String[] combinedArgs = new String[selectionArgs.length + 1];
                        combinedArgs[0] = String.valueOf(id);
                        System.arraycopy(selectionArgs, 0, combinedArgs, 1, selectionArgs.length);
                        idArgs = combinedArgs;
                    }
                }
                rowsUpdated = db.update(DatabaseHelper.TABLE_TASKS, values, idSelection, idArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported update URI: " + uri);
        }

        if (rowsUpdated > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        Log.d(TAG, "update: " + rowsUpdated + " row(s) updated for URI: " + uri);
        return rowsUpdated;
    }
}
