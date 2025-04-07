package hcmute.edu.vn.selfalarm.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CallDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "calls.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_CALLS = "calls";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NUMBER = "number";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_TYPE = "type";

    private static final String DATABASE_CREATE = "create table " + TABLE_CALLS
            + "(" + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_NUMBER + " text not null, "
            + COLUMN_DATE + " integer not null, "
            + COLUMN_DURATION + " integer not null, "
            + COLUMN_TYPE + " integer not null);";

    public CallDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALLS);
        onCreate(db);
    }
} 