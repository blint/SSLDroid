package hu.blint.ssldroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class SSLDroidReadLogs extends Activity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem refresh = menu.add(R.string.refresh);
        refresh.setIcon(android.R.drawable.ic_menu_rotate);
        MenuItem share = menu.add(R.string.share);
        share.setIcon(android.R.drawable.ic_menu_share);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle() == getResources().getString(R.string.refresh)) 
            refreshLogs();
        else if ((item.getTitle() == getResources().getString(R.string.share)))
            shareLogs();
        else
            return false;
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.read_logs);
        refreshLogs();
    }

    public void refreshLogs() {
        TextView logcontainer = (TextView) findViewById(R.id.logTextView);
        logcontainer.setText("");
        Process mLogcatProc = null;
        BufferedReader reader = null;
        try {
            mLogcatProc = Runtime.getRuntime().exec(new String[]
                                                    {"logcat", "-d", "-v", "time", "-b", "main", "SSLDroid:D SSLDroidGui:D AndroidRuntime *:S" });

            reader = new BufferedReader(new InputStreamReader(mLogcatProc.getInputStream()));

            String line;
            String separator = System.getProperty("line.separator");

            while ((line = reader.readLine()) != null) {
                logcontainer.append(line+separator);
            }
        } catch (IOException e) {
            Log.d("SSLDroid", "Logcat problem: "+e.toString());
        }
        finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.d("SSLDroid", "Logcat problem: "+e.toString());
                }
        }
    }
    
    public void shareLogs() {
	Intent sendIntent = new Intent();
	TextView logcontainer = (TextView) findViewById(R.id.logTextView);
	CharSequence logdata = logcontainer.getText();
	
	sendIntent.setAction(Intent.ACTION_SEND);
	sendIntent.putExtra(Intent.EXTRA_TEXT, logdata);
	sendIntent.setType("text/plain");
	startActivity(sendIntent);
    }

}
