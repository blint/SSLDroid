package hu.blint.ssldroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootStartupReceiver extends BroadcastReceiver{
@Override
public void onReceive(Context context, Intent intent) {
	Intent serviceIntent = new Intent();
	serviceIntent.setAction("hu.blint.ssldroid.SSLDroid");
	context.startService(serviceIntent);
}
}