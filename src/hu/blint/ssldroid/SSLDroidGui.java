package hu.blint.ssldroid;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SSLDroidGui extends Activity implements OnClickListener {
	private static final String TAG = "SSLDroidGui";
	public static final String PREFS_NAME = "MyPrefsFile";
	Button buttonStart, buttonStop, buttonApply;

	public boolean saveSettings(){
	      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	      SharedPreferences.Editor editor = settings.edit();
	      
	      TextView localport = (TextView) findViewById(R.id.localport);
	      TextView remotehost = (TextView) findViewById(R.id.remotehost);
	      TextView remoteport = (TextView) findViewById(R.id.remoteport);
	      TextView pkcsfile = (TextView) findViewById(R.id.pkcsfile);
	      TextView pkcspass = (TextView) findViewById(R.id.pkcspass);
	      
		  String settingLocalport = localport.getText().toString();
		  String settingRemotehost = remotehost.getText().toString();
		  String settingRemoteport = remoteport.getText().toString();
		  String settingPkcsfile = pkcsfile.getText().toString();
		  String settingPkcspass = pkcspass.getText().toString();
	      
		  if (settingLocalport.length() == 0) {
			  Toast.makeText(this, "Required local port parameter not setup, skipping save", 5).show();
			  return false;
		  }
		  if (settingRemotehost.length() == 0){
			  Toast.makeText(this, "Required remote host parameter not setup, skipping save", 5).show();
			  return false;
		  }
		  if (settingRemoteport.length() == 0){
			  Toast.makeText(this, "Required remote port parameter not setup, skipping save", 5).show();
			  return false;
		  }
		  if (settingPkcsfile.length() == 0){
			  Toast.makeText(this, "Required PKCS12 file parameter not setup, skipping save", 5).show();
			  return false;
		  }	  
		  editor.putInt("0.localPort", Integer.parseInt(settingLocalport));
		  //Log.d(TAG, "settingSave: '"+ settingLocalport+"'");
		  editor.putString("0.remoteHost", settingRemotehost);
		  //Log.d(TAG, "settingSave: '"+ settingRemotehost+"'");
		  editor.putInt("0.remotePort", Integer.parseInt(settingRemoteport));
		  //Log.d(TAG, "settingSave: '"+ settingRemoteport+"'");
		  editor.putString("0.pkcsFile", settingPkcsfile);
		  //Log.d(TAG, "settingSave: '"+ settingPkcsfile+"'");
		  editor.putString("0.pkcsPass", settingPkcspass);
		  //Log.d(TAG, "settingSave: '"+ settingPkcspass+"'");
		  editor.commit();
		  return true;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonStop = (Button) findViewById(R.id.buttonStop);
		buttonApply = (Button) findViewById(R.id.buttonApply);

		buttonStart.setOnClickListener(this);
		buttonStop.setOnClickListener(this);
		buttonApply.setOnClickListener(this);
		
	    TextView localport = (TextView) findViewById(R.id.localport);
	    TextView remotehost = (TextView) findViewById(R.id.remotehost);
	    TextView remoteport = (TextView) findViewById(R.id.remoteport);
	    TextView pkcsfile = (TextView) findViewById(R.id.pkcsfile);
	    TextView pkcspass = (TextView) findViewById(R.id.pkcspass);
		
	    // Restore preferences
	    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    int settingLocalport = settings.getInt("0.localPort", 0);
	    String settingRemotehost = settings.getString("0.remoteHost", "");
	    int settingRemoteport = settings.getInt("0.remotePort", 0);
	    String settingPkcsfile = settings.getString("0.pkcsFile", "");
	    String settingPkcspass = settings.getString("0.pkcsPass", "");
	    
	    if (settingLocalport!=0)
	    	localport.setText(String.valueOf(settingLocalport));
	    if (settingRemotehost!="")
	    	remotehost.setText(settingRemotehost);
	    if (settingRemoteport!=0)
	    	remoteport.setText(String.valueOf(settingRemoteport));
	    if (settingPkcsfile!="")
	    	pkcsfile.setText(settingPkcsfile);
	    if (settingPkcspass!="")
	    	pkcspass.setText(settingPkcspass);	    
	}

	public void onClick(View src) {
		switch (src.getId()) {
		case R.id.buttonStart:
			Log.d(TAG, "Starting service");
			startService(new Intent(this, SSLDroid.class));
			break;
		case R.id.buttonStop:
			Log.d(TAG, "Stopping service");
			stopService(new Intent(this, SSLDroid.class));
			break;
		case R.id.buttonApply:
			Log.d(TAG, "Saving settings...");
			if (saveSettings()){
				Log.d(TAG, "Restarting service after setting save");
				stopService(new Intent(this, SSLDroid.class));
				startService(new Intent(this, SSLDroid.class));
			}
			break;
		}
	}
}