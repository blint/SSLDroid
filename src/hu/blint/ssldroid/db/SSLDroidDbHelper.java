package hu.blint.ssldroid.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SSLDroidDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "applicationdata";
    private static final int DATABASE_VERSION = 3;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS tunnels (_id integer primary key autoincrement, "
            + "name text not null, localport integer not null, remotehost text not null, "
            + "remoteport integer not null, pkcsfile text not null, pkcspass text, cacertfile text );";
    private static final String STATUS_CREATE = "CREATE TABLE IF NOT EXISTS status (name text, value text);";

    public SSLDroidDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        database.execSQL(STATUS_CREATE);
    }

    // Method is called during an update of the database, e.g. if you increase
    // the database version
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion,
                          int newVersion) {
        Log.w(SSLDroidDbHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will add a status table");
        database.execSQL("CREATE TABLE IF NOT EXISTS status (name text, value text);");
        if (oldVersion < 3)
            database.execSQL("ALTER TABLE tunnels ADD cacertfile text;");
        onCreate(database);
    }
}

