package hcmute.edu.vn.selfalarm.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import hcmute.edu.vn.selfalarm.data.CallDbHelper;

public class CallContentProvider extends ContentProvider {
    public static final String AUTHORITY = "hcmute.edu.vn.sms_and_call.providers.call";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/calls");

    private static final int CALLS = 1;
    private static final int CALL_ID = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, "calls", CALLS);
        uriMatcher.addURI(AUTHORITY, "calls/#", CALL_ID);
    }

    private CallDbHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new CallDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                       @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        switch (uriMatcher.match(uri)) {
            case CALLS:
                cursor = db.query("calls", projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case CALL_ID:
                String id = uri.getLastPathSegment();
                cursor = db.query("calls", projection, "_id = ?", new String[]{id}, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (uriMatcher.match(uri) != CALLS) {
            throw new IllegalArgumentException("Invalid URI for insert");
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert("calls", null, values);

        if (id > 0) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, id);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }

        throw new android.database.SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;

        switch (uriMatcher.match(uri)) {
            case CALLS:
                count = db.delete("calls", selection, selectionArgs);
                break;
            case CALL_ID:
                String id = uri.getLastPathSegment();
                count = db.delete("calls", "_id = ?", new String[]{id});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                     @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;

        switch (uriMatcher.match(uri)) {
            case CALLS:
                count = db.update("calls", values, selection, selectionArgs);
                break;
            case CALL_ID:
                String id = uri.getLastPathSegment();
                count = db.update("calls", values, "_id = ?", new String[]{id});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
} 