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
	private static final String DATABASE_TABLE = "tunnels";
	private Context context;
	private SQLiteDatabase database;
	private SSLDroidDbHelper dbHelper;

	public SSLDroidDbAdapter(Context context) {
		this.context = context;
	}

	public SSLDroidDbAdapter open() throws SQLException {
		dbHelper = new SSLDroidDbHelper(context);
		database = dbHelper.getWritableDatabase();
		return this;
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
						   String pkcsfile, String pkcspass) {
		ContentValues initialValues = createContentValues(name, localport, remotehost,
														  remoteport, pkcsfile, pkcspass);

		return database.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Update the tunnel
	 */
	public boolean updateTunnel(long rowId, String name, int localport, String remotehost, 
							  int remoteport, String pkcsfile, String pkcspass) {
		ContentValues updateValues = createContentValues(name, localport, remotehost,
				  remoteport, pkcsfile, pkcspass);

		return database.update(DATABASE_TABLE, updateValues, KEY_ROWID + "="
				+ rowId, null) > 0;
	}

	/**
	 * Deletes tunnel
	 */
	public boolean deleteTunnel(long rowId) {
		return database.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all tunnel in the database
	 * 
	 * @return Cursor over all notes
	 */
	public Cursor fetchAllTunnels() {
		return database.query(DATABASE_TABLE, new String[] { KEY_ROWID,
				KEY_NAME, KEY_LOCALPORT, KEY_REMOTEHOST, KEY_REMOTEPORT, KEY_PKCSFILE,
				KEY_PKCSPASS }, null, null, null, null, null);
	}

	/**
	 * Return a Cursor positioned at the defined tunnel
	 */
	public Cursor fetchTunnel(long rowId) throws SQLException {
		Cursor mCursor = database.query(true, DATABASE_TABLE, new String[] {
				KEY_ROWID, KEY_NAME, KEY_LOCALPORT, KEY_REMOTEHOST, KEY_REMOTEPORT, 
				KEY_PKCSFILE, KEY_PKCSPASS },
				KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	private ContentValues createContentValues(String name, int localport, String remotehost, int remoteport, 
			   String pkcsfile, String pkcspass) {
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


