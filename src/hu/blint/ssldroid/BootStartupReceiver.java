package hu.blint.ssldroid;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

public class BootStartupReceiver extends BroadcastReceiver {

    public void createNetworkChangeListener(Context context){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            context.registerReceiver(new NetworkChangeReceiver(null), new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        }
        else {
            Intent startServiceIntent = new Intent(context, NetworkChangeService.class);
            context.startService(startServiceIntent);
            Log.d("SSLDroid", "Scheduling network change monitor job");
            JobInfo myJob = new JobInfo.Builder(0, new ComponentName(context, NetworkChangeService.class))
                    .setRequiresCharging(true)
                    .setMinimumLatency(1000)
                    .setOverrideDeadline(2000)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build();

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(myJob);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != Intent.ACTION_BOOT_COMPLETED)
            return;

        createNetworkChangeListener(context);

        Intent i = new Intent(context, SSLDroid.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i);
        } else {
            context.startService(i);
        }
    }
}