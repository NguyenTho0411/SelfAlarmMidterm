package hcmute.edu.vn.selfalarm.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BlacklistDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "blacklist.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_BLACKLIST = "blacklist";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PHONE_NUMBER = "phone_number";

    private static final String DATABASE_CREATE = "create table " + TABLE_BLACKLIST
            + "(" + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_PHONE_NUMBER + " text not null unique);";

    public BlacklistDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BLACKLIST);
        onCreate(db);
    }
}