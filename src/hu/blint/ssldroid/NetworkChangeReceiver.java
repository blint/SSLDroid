package hu.blint.ssldroid;

import hu.blint.ssldroid.db.SSLDroidDbAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private boolean isStopped(Context context){
	boolean stopped = false;
	SSLDroidDbAdapter dbHelper;
	dbHelper = new SSLDroidDbAdapter(context);
        dbHelper.open();
        Cursor cursor = dbHelper.getStopStatus();

        int tunnelcount = cursor.getCount();
        Log.d("SSLDroid", "Tunnelcount: "+tunnelcount);
        
        //don't start if the stop status field is available
        if (tunnelcount != 0){
            stopped = true;
        }
        
        cursor.close();
        dbHelper.close();
        
	return stopped;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService( Context.CONNECTIVITY_SERVICE );
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if ( activeNetInfo == null ) {
            Intent i = new Intent();
            i.setAction("hu.blint.ssldroid.SSLDroid");
            context.stopService(i);
            return;
        }
        Log.d("SSLDroid", activeNetInfo.toString());
        if (activeNetInfo.isAvailable()) {
            Intent i = new Intent();
            i.setAction("hu.blint.ssldroid.SSLDroid");
            context.stopService(i);
            if (!isStopped(context))
        	context.startService(i);
            else
        	Log.w("SSLDroid", "Not starting service as directed by explicit stop");
        }
    }
}

