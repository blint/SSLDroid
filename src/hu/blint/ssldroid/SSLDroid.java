package hu.blint.ssldroid;

import hu.blint.ssldroid.TcpProxy;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import hu.blint.ssldroid.db.SSLDroidDbAdapter;

public class SSLDroid extends Service {

    private final String TAG = "SSLDroid";
    private TcpProxy[] tp;
    private SSLDroidDbAdapter dbHelper = new SSLDroidDbAdapter(this);

    @Override
    public void onCreate() {
	    //initialize secure random Generation
	    PRNGFixes.apply();

        dbHelper.open();
        Cursor cursor = dbHelper.fetchAllTunnels();

        int tunnelcount = cursor.getCount();

        //skip start if the db is empty yet
        if (tunnelcount == 0)
            return;

        tp = new TcpProxy[tunnelcount];

        int i;
        for (i=0; i<tunnelcount; i++) {
            cursor.moveToPosition(i);
            String tunnelName = cursor.getString(cursor
                                                 .getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_NAME));
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
            String caFile = cursor.getString(cursor
                                              .getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_CACERTFILE));
            boolean useSNI = true;
            int useSNIrepr = cursor.getInt(cursor.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_USE_SNI));
            if (useSNIrepr == 0)
                useSNI = false;

            try {
                tp[i] = new TcpProxy(tunnelName, listenPort, targetHost, targetPort, keyFile, keyPass, caFile, useSNI);
                tp[i].serve();
                Log.d(TAG, "Tunnel: "+tunnelName+" "+listenPort+" "+targetHost+" "+targetPort+" "+keyFile);
            } catch (Exception e) {
                Log.d(TAG, "Error:" + e.toString());
                new AlertDialog.Builder(SSLDroid.this)
                .setTitle("SSLDroid encountered a fatal error: "+e.getMessage())
                .setPositiveButton(android.R.string.ok, null)
                .create();
            }
        }

        cursor.close();
        dbHelper.close();
        createNotification(0, true, "SSLDroid is running", "Started and serving "+tunnelcount+" tunnels");
        //get the version
        int vcode = 0;
        String vname = "";
        PackageManager manager = this.getPackageManager();
        try {
	    PackageInfo pkginfo = manager.getPackageInfo(this.getPackageName(), 0);
	    vname = pkginfo.versionName;
	    vcode = pkginfo.versionCode;
	} catch (NameNotFoundException e) {
	    Log.d(TAG, "Error getting package version; error='"+e.toString()+"'");
	}
        //startup message
        Log.d(TAG, "SSLDroid Service Started; version='"+vcode +"', versionname='"+vname+"'");
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
        super.onDestroy();
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

    private void removeNotification(int id) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.cancel(id);
    }

    private void createNotification(int id, boolean persistent, String title, String text) {

        Context context = getApplicationContext();
        Intent mainIntent = new Intent(context, SSLDroidGui.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.icon)
                .setContentTitle(title)
                .setContentText(text)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(Notification.PRIORITY_HIGH);
        if (persistent)
            builder.setOngoing(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "REMINDERS";
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Reminder",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }
        notificationManager.notify(id, builder.build());
    }
}
