package hu.blint.ssldroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {

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
            context.startService(i);
        }
    }
}

