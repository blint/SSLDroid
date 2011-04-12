package hu.blint.ssldroid;

import hu.blint.ssldroid.TcpProxy;
import android.app.*;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SSLDroid extends Service {

	final String TAG = "SSLDroid";
	TcpProxy tp;

	@Override
	public void onCreate() {
		int listenPort = 9999; // port to listen on
		int targetPort = 443; // port to connect to
		String targetHost = "sogo.balabit.com"; // remote host
		String keyFile = "/mnt/sdcard/blint-imaps.p12";
		String keyPass = "titkos";

		//Toast.makeText(this, "SSLDroid Service Started", Toast.LENGTH_LONG).show();
		createNotification("SSLDroid is running", "SSLDroid service is running");
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
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			notificationManager.cancel(0);
			Log.d(TAG, "SSLDroid Service Stopped");
		} catch (Exception e) {
			Log.d("SSLDroid", "Error stopping service: " + e.toString());
		}
	}

	public void createNotification(String title, String text) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon,
				"SSLDroid startup", System.currentTimeMillis());
		// Hide the notification after its selected
		//notification.flags |= Notification.FLAG_AUTO_CANCEL;

		Intent intent = new Intent(this, SSLDroidGui.class);
		PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 0);
		notification.setLatestEventInfo(this, title, text, activity);
		notificationManager.notify(0, notification);
	}
}

/*
public class MyStartupIntentReceiver extends BroadcastReceiver{
	@Override
	public void onReceive(Context context, Intent intent) {
	}
			Intent serviceIntent = new Intent();
		serviceIntent.setAction("hu.blint.ssldroid");
		context.startService(serviceIntent);
	}

*/
