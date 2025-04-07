package hcmute.edu.vn.selfalarm.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import hcmute.edu.vn.selfalarm.data.SmsDbHelper;

public class SmsContentProvider extends ContentProvider {
    private static final String TAG = "SmsContentProvider";
    private static final String AUTHORITY = "hcmute.edu.vn.sms_and_call.providers.sms";
    private static final String BASE_PATH = "sms";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static final int SMS = 1;
    private static final int SMS_ID = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, BASE_PATH, SMS);
        uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SMS_ID);
    }

    private SQLiteOpenHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new SmsDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        switch (uriMatcher.match(uri)) {
            case SMS:
                cursor = db.query(SmsDbHelper.TABLE_SMS, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case SMS_ID:
                String id = uri.getLastPathSegment();
                cursor = db.query(SmsDbHelper.TABLE_SMS, projection, SmsDbHelper.COLUMN_ID + "=?", 
                        new String[]{id}, null, null, sortOrder);
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
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(SmsDbHelper.TABLE_SMS, null, values);
        Uri newUri = ContentUris.withAppendedId(CONTENT_URI, id);
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)) {
            case SMS:
                count = db.delete(SmsDbHelper.TABLE_SMS, selection, selectionArgs);
                break;
            case SMS_ID:
                String id = uri.getLastPathSegment();
                count = db.delete(SmsDbHelper.TABLE_SMS, SmsDbHelper.COLUMN_ID + "=?", new String[]{id});
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
            case SMS:
                count = db.update(SmsDbHelper.TABLE_SMS, values, selection, selectionArgs);
                break;
            case SMS_ID:
                String id = uri.getLastPathSegment();
                count = db.update(SmsDbHelper.TABLE_SMS, values, SmsDbHelper.COLUMN_ID + "=?", new String[]{id});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
} 