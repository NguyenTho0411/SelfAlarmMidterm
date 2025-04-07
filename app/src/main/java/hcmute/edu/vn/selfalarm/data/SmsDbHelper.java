package hcmute.edu.vn.selfalarm.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SmsDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "sms.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_SMS = "sms";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TYPE = "type";

    private static final String DATABASE_CREATE = "create table " + TABLE_SMS
            + "(" + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_ADDRESS + " text not null, "
            + COLUMN_BODY + " text not null, "
            + COLUMN_DATE + " integer not null, "
            + COLUMN_TYPE + " integer not null);";

    public SmsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SMS);
        onCreate(db);
    }
} 