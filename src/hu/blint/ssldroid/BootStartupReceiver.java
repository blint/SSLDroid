package hu.blint.ssldroid;

import hu.blint.ssldroid.db.SSLDroidDbAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

public class BootStartupReceiver extends BroadcastReceiver {
    
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
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent();
            i.setAction("hu.blint.ssldroid.SSLDroid");
            if (!isStopped(context))
                context.startService(i);
            else
        	Log.w("SSLDroid", "Not starting service as directed by explicit stop");
        }
    }
}