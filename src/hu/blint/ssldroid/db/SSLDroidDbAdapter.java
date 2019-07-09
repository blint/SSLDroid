package hu.blint.ssldroid.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class SSLDroidDbAdapter {

    // Database fields
    public static final String KEY_ROWID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_LOCALPORT = "localport";
    public static final String KEY_REMOTEHOST = "remotehost";
    public static final String KEY_REMOTEPORT = "remoteport";
    public static final String KEY_PKCSFILE = "pkcsfile";
    public static final String KEY_PKCSPASS = "pkcspass";
    public static final String KEY_CACERTFILE = "cacertfile";
    public static final String KEY_USE_SNI = "usesni";
    private static final String KEY_STATUS_NAME = "name";
    private static final String KEY_STATUS_VALUE = "value";
    private static final String DATABASE_TABLE = "tunnels";
    private static final String STATUS_TABLE = "status";
    private final Context context;
    private SQLiteDatabase database;
    private SSLDroidDbHelper dbHelper;

    public SSLDroidDbAdapter(Context context) {
        this.context = context;
    }

    public void open() throws SQLException {
        dbHelper = new SSLDroidDbHelper(context);
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
        database.close();
    }

    /**
     * Create a new tunnel If the tunnel is successfully created return the new
     * rowId for that note, otherwise return a -1 to indicate failure.
     */
    public long createTunnel(String name, int localport, String remotehost, int remoteport,
                             String pkcsfile, String pkcspass, String cacertfile, int usesni) {
        ContentValues initialValues = createContentValues(name, localport, remotehost,
                                      remoteport, pkcsfile, pkcspass, cacertfile, usesni);

        return database.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Update the tunnel
     */
    public void updateTunnel(long rowId, String name, int localport, String remotehost,
                                int remoteport, String pkcsfile, String pkcspass, String cacertfile, int usesni) {
        ContentValues updateValues = createContentValues(name, localport, remotehost,
                                     remoteport, pkcsfile, pkcspass, cacertfile, usesni);

        database.update(DATABASE_TABLE, updateValues, KEY_ROWID + "="
                + rowId, null);
    }

    /**
     * Deletes tunnel
     */
    public void deleteTunnel(long rowId) {
        database.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null);
    }

    /**
     * Return a Cursor over the list of all tunnels in the database
     *
     * @return Cursor over all notes
     */
    public Cursor fetchAllTunnels() {
        return database.query(DATABASE_TABLE, new String[] { KEY_ROWID,
                              KEY_NAME, KEY_LOCALPORT, KEY_REMOTEHOST, KEY_REMOTEPORT, KEY_PKCSFILE,
                              KEY_PKCSPASS, KEY_CACERTFILE, KEY_USE_SNI
                                                           }, null, null, null, null, null);
    }

    /**
     * Return a Cursor over the list of all tunnels in the database
     *
     * @return Cursor over all notes
     */
    public Cursor fetchAllLocalPorts() {
        return database.query(DATABASE_TABLE, new String[] { KEY_NAME,
                              KEY_LOCALPORT
                                                           }, null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the defined tunnel
     */
    private Cursor fetchStatus(String valuename) throws SQLException {
        return database.query(STATUS_TABLE, new String[] {
                                            KEY_STATUS_NAME, KEY_STATUS_VALUE
                                        },
                                        KEY_STATUS_NAME + "='" + valuename + "'", null, null, null, null);
    }

    public Cursor getStopStatus() {
        return fetchStatus("stopped");
    }

    @SuppressWarnings("SameReturnValue")
    public void setStopStatus() {
	ContentValues stopStatus = new ContentValues();
        stopStatus.put(KEY_STATUS_NAME, "stopped");
        stopStatus.put(KEY_STATUS_VALUE, "yes");
        if (getStopStatus().getCount() == 0)
            database.insert(STATUS_TABLE, null, stopStatus);
    }
    
    public void delStopStatus() {
        database.delete(STATUS_TABLE, KEY_STATUS_NAME + "= 'stopped'", null);
    }
    
    public Cursor fetchTunnel(long rowId) throws SQLException {
        Cursor mCursor = database.query(true, DATABASE_TABLE, new String[] {
                                            KEY_ROWID, KEY_NAME, KEY_LOCALPORT, KEY_REMOTEHOST, KEY_REMOTEPORT,
                                            KEY_PKCSFILE, KEY_PKCSPASS, KEY_CACERTFILE, KEY_USE_SNI
                                        },
                                        KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
    
    private ContentValues createContentValues(String name, int localport, String remotehost, int remoteport,
            String pkcsfile, String pkcspass, String cacertfile, int usesni) {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_LOCALPORT, localport);
        values.put(KEY_REMOTEHOST, remotehost);
        values.put(KEY_REMOTEPORT, remoteport);
        values.put(KEY_REMOTEPORT, remoteport);
        values.put(KEY_PKCSFILE, pkcsfile);
        values.put(KEY_PKCSPASS, pkcspass);
        return values;
    }
}


