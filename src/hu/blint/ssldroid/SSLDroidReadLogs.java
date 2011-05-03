package hu.blint.ssldroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class SSLDroidReadLogs extends Activity{

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.read_logs);
    TextView logcontainer = (TextView) findViewById(R.id.logTextView);
/*	try {
		int[] tags = new int[1];
		tags[0] = 0;
		Collection<EventLog.Event> output = new HashSet<EventLog.Event>();
		EventLog eventlog = new EventLog();
	    eventlog.readEvents(tags, output);
	    //Log.i("test",Integer.toString(desc.mTag));
	    logcontainer.setText("wtf");
	    Iterator<EventLog.Event> logs = output.iterator();
	    while (logs.hasNext()){
	    	Log.d("SSLDroid", "Entry");
	    	EventLog.Event entry = logs.next();
	    	logcontainer.append(entry.getData().toString());
	    }
	} catch (IOException e) {
		e.printStackTrace();
	} */
    
    Process mLogcatProc = null;
    BufferedReader reader = null;
    try
    {
            mLogcatProc = Runtime.getRuntime().exec(new String[]
                    {"logcat", "-d", "SSLDroid:D SSLDroidGui:D *:S" });

            reader = new BufferedReader(new InputStreamReader(mLogcatProc.getInputStream()));

            String line;
            final StringBuilder log = new StringBuilder();
            String separator = System.getProperty("line.separator"); 

            while ((line = reader.readLine()) != null)
            {
                    log.append(line);
                    log.append(separator);
            }

            logcontainer.setText(log);
    }

    catch (IOException e)
    {
            Log.d("SSLDroid", "Logcat problem: "+e.toString());
    }

    finally
    {
            if (reader != null)
                    try
                    {
                            reader.close();
                    }
                    catch (IOException e)
                    {
                        Log.d("SSLDroid", "Logcat problem: "+e.toString());
                    }

    }
    
  }
  
}
