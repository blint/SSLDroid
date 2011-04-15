package hu.blint.ssldroid;

import hu.blint.ssldroid.TcpProxy;
import android.app.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class SSLDroid extends Service {

	final String TAG = "SSLDroid";
	public static final String PREFS_NAME = "MyPrefsFile";
	TcpProxy tp;

	@Override
	public void onCreate() {
		
	    // Restore preferences
	    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    int settingLocalport = settings.getInt("0.localPort", 0);
	    String settingRemotehost = settings.getString("0.remoteHost", "");
	    int settingRemoteport = settings.getInt("0.remotePort", 0);
	    String settingPkcsfile = settings.getString("0.pkcsFile", "");
	    String settingPkcspass = settings.getString("0.pkcsPass", "");
	    
	    int listenPort;
		int targetPort;
		String targetHost;
		String keyFile;
		String keyPass;
	    
	    if (settingLocalport!=0)
	    	listenPort = settingLocalport;
	    else {
	    	Toast.makeText(this, "Please set up local port first", Toast.LENGTH_LONG).show();
	    	return;
	    }
	    if (settingRemotehost!="")
	    	targetHost = settingRemotehost;
	    else {
	    	Toast.makeText(this, "Please set up remote host first", Toast.LENGTH_LONG).show();
	    	return;
	    }
	    if (settingRemoteport!=0)
	    	targetPort = settingRemoteport;
	    else {
	    	Toast.makeText(this, "Please set up remote port first", Toast.LENGTH_LONG).show();
	    	return;
	    }
	    if (settingPkcsfile!="")
	    	keyFile = settingPkcsfile;
	    else {
	    	Toast.makeText(this, "Please set up PKCS12 file first", Toast.LENGTH_LONG).show();
	    	return;
	    }
    	keyPass = settingPkcspass;
		
		//Toast.makeText(this, "SSLDroid Service Started", Toast.LENGTH_LONG).show();
		createNotification(0, "SSLDroid is running", "SSLDroid service is running");
		Log.d(TAG, "SSLDroid Service Started");

		//createNotification("test", "This is a test of the emergency broadcast system");

		tp = new TcpProxy();
		try {
			tp.serve(listenPort, targetHost, targetPort, keyFile, keyPass);
		} catch (Exception e) {
			Log.d(TAG, "Error" + e.toString());
		}
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
		try {
			tp.stop();
			removeNotification(0);
			Log.d(TAG, "SSLDroid Service Stopped");
		} catch (Exception e) {
			Log.d("SSLDroid", "Error stopping service: " + e.toString());
		}
	}

	public void removeNotification(int id){
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(id);
	}
	
	public void createNotification(int id, String title, String text) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon,
				"SSLDroid startup", System.currentTimeMillis());
		// Hide the notification after its selected
		//notification.flags |= Notification.FLAG_AUTO_CANCEL;

		Intent intent = new Intent(this, SSLDroidGui.class);
		PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 0);
		notification.setLatestEventInfo(this, title, text, activity);
		notificationManager.notify(id, notification);
	}
}
