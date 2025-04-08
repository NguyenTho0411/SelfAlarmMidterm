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

import hcmute.edu.vn.selfalarm.data.BlacklistDbHelper;

public class BlacklistContentProvider extends ContentProvider {

    public static final String AUTHORITY = "hcmute.edu.vn.selfalarm.providers.blacklist";
    public static final String BASE_PATH = "blacklist";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static final int BLACKLIST_ENTRIES = 1;
    private static final int BLACKLIST_ENTRY_NUMBER = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, BASE_PATH, BLACKLIST_ENTRIES);
        uriMatcher.addURI(AUTHORITY, BASE_PATH + "/number/*", BLACKLIST_ENTRY_NUMBER);
    }

    private BlacklistDbHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new BlacklistDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        int match = uriMatcher.match(uri);

        switch (match) {
            case BLACKLIST_ENTRIES:
                cursor = db.query(BlacklistDbHelper.TABLE_BLACKLIST, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case BLACKLIST_ENTRY_NUMBER:
                String number = uri.getLastPathSegment();
                cursor = db.query(BlacklistDbHelper.TABLE_BLACKLIST, projection,
                        BlacklistDbHelper.COLUMN_PHONE_NUMBER + "=?", new String[]{number}, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (uriMatcher.match(uri) != BLACKLIST_ENTRIES) {
            throw new IllegalArgumentException("Invalid URI for insertion: " + uri);
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insertOrThrow(BlacklistDbHelper.TABLE_BLACKLIST, null, values);
        if (id > 0) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, id);
            getContext().getContentResolver().notifyChange(uri, null);
            return newUri;
        }
        throw new android.database.SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        int match = uriMatcher.match(uri);

        switch (match) {
            case BLACKLIST_ENTRIES:
                count = db.delete(BlacklistDbHelper.TABLE_BLACKLIST, selection, selectionArgs);
                break;
            case BLACKLIST_ENTRY_NUMBER:
                String number = uri.getLastPathSegment();
                count = db.delete(BlacklistDbHelper.TABLE_BLACKLIST, BlacklistDbHelper.COLUMN_PHONE_NUMBER + "=?", new String[]{number});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Update not supported for this provider");
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }
}