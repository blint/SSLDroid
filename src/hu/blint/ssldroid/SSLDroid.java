package hu.blint.ssldroid;

import hu.blint.ssldroid.TcpProxy;
import android.app.*;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;
import hu.blint.ssldroid.db.SSLDroidDbAdapter;

public class SSLDroid extends Service {

	final String TAG = "SSLDroid";
	TcpProxy tp[];
	private SSLDroidDbAdapter dbHelper;

	@Override
	public void onCreate() {
		
		dbHelper = new SSLDroidDbAdapter(this);
		dbHelper.open();
		Cursor cursor = dbHelper.fetchAllTunnels();
		
		int tunnelcount = cursor.getCount();
		
		//skip start if the db is empty yet
		if (tunnelcount == 0)
			return;
		
		tp = new TcpProxy[tunnelcount];
		
		int i;
		for (i=0; i<tunnelcount; i++){
			cursor.moveToPosition(i);
		    int listenPort = cursor.getInt(cursor
					.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_LOCALPORT));
			int targetPort = cursor.getInt(cursor
					.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_REMOTEPORT));
			String targetHost = cursor.getString(cursor
					.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_REMOTEHOST));
			String keyFile = cursor.getString(cursor
					.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_PKCSFILE));
			String keyPass = cursor.getString(cursor
					.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_PKCSPASS));
			try {
				tp[i] = new TcpProxy(listenPort, targetHost, targetPort, keyFile, keyPass);
				tp[i].serve();
				Log.d(TAG, "Tunnel: "+listenPort+" "+targetHost+" "+targetPort+" "+keyFile);
			} catch (Exception e) {
				Log.d(TAG, "Error:" + e.toString());
				new AlertDialog.Builder(SSLDroid.this)
                .setTitle("SSLDroid encountered a fatal error: "+e.getMessage())
                .setPositiveButton(android.R.string.ok, null)
                .create();
			}
		}
	    
		cursor.deactivate();
		cursor.close();
		dbHelper.close();
		createNotification(0, true, "SSLDroid is running", "Started and serving "+tunnelcount+" tunnels");
		Log.d(TAG, "SSLDroid Service Started");	
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		dbHelper.close();
		try {
			for (TcpProxy proxy : tp) {
	            proxy.stop();
	        }
		} catch (Exception e) {
			Log.d("SSLDroid", "Error stopping service: " + e.toString());
		}
		removeNotification(0);
		Log.d(TAG, "SSLDroid Service Stopped");
	}

	public void removeNotification(int id){
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(id);
	}
	
	public void createNotification(int id, boolean persistent, String title, String text) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon,
				"SSLDroid startup", System.currentTimeMillis());
		// if requested, make the notification persistent, e.g. not clearable by the user at all,
		// automatically hide on displaying the main activity otherwise
		if (persistent == true)
			notification.flags |= Notification.FLAG_NO_CLEAR;
		else
			notification.flags |= Notification.FLAG_AUTO_CANCEL;

		Intent intent = new Intent(this, SSLDroidGui.class);
		PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 0);
		notification.setLatestEventInfo(this, title, text, activity);
		notificationManager.notify(id, notification);
	}
}
