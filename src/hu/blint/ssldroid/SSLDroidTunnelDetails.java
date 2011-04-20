package hu.blint.ssldroid;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import hu.blint.ssldroid.db.SSLDroidDbAdapter;

public class SSLDroidTunnelDetails extends Activity {
	private EditText name;
	private EditText localport;
	private EditText remotehost;
	private EditText remoteport;
	private EditText pkcsfile;
	private EditText pkcspass;
	private Long rowId;
	private SSLDroidDbAdapter dbHelper;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		dbHelper = new SSLDroidDbAdapter(this);
		dbHelper.open();
		setContentView(R.layout.tunnel_details);

		Button confirmButton = (Button) findViewById(R.id.tunnel_apply_button);
		name = (EditText) findViewById(R.id.name);
		localport = (EditText) findViewById(R.id.localport);
		remotehost = (EditText) findViewById(R.id.remotehost);
		remoteport = (EditText) findViewById(R.id.remoteport);
		pkcsfile = (EditText) findViewById(R.id.pkcsfile);
		pkcspass = (EditText) findViewById(R.id.pkcspass);
		
		rowId = null;
		Bundle extras = getIntent().getExtras();
		rowId = (bundle == null) ? null : (Long) bundle
				.getSerializable(SSLDroidDbAdapter.KEY_ROWID);
		if (extras != null) {
			rowId = extras.getLong(SSLDroidDbAdapter.KEY_ROWID);
		}
		populateFields();
		confirmButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				//TODO: put input validation here
				//TODO: put local port collision check here
				if (name.getText().length() == 0) {
					  Toast.makeText(getBaseContext(), "Required tunnel name parameter not setup, skipping save", 5).show();
					  return;
				  }
				  if (localport.getText().length() == 0) {
					  Toast.makeText(getBaseContext(), "Required local port parameter not setup, skipping save", 5).show();
					  return;
				  }
				  if (remotehost.getText().length() == 0){
					  Toast.makeText(getBaseContext(), "Required remote host parameter not setup, skipping save", 5).show();
					  return;
				  }
				  if (remoteport.getText().length() == 0){
					  Toast.makeText(getBaseContext(), "Required remote port parameter not setup, skipping save", 5).show();
					  return;
				  }
				  if (pkcsfile.getText().length() == 0){
					  Toast.makeText(getBaseContext(), "Required PKCS12 file parameter not setup, skipping save", 5).show();
					  return;
				  }
				setResult(RESULT_OK);
				finish();
			}

		});
	}

	private void populateFields() {
		if (rowId != null) {
			Cursor Tunnel = dbHelper.fetchTunnel(rowId);
			startManagingCursor(Tunnel);
			
			name.setText(Tunnel.getString(Tunnel
					.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_NAME)));
			localport.setText(Tunnel.getString(Tunnel
					.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_LOCALPORT)));
			remotehost.setText(Tunnel.getString(Tunnel
					.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_REMOTEHOST)));
			remoteport.setText(Tunnel.getString(Tunnel
					.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_REMOTEPORT)));
			pkcsfile.setText(Tunnel.getString(Tunnel
					.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_PKCSFILE)));
			pkcspass.setText(Tunnel.getString(Tunnel
					.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_PKCSPASS)));
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState();
		outState.putSerializable(SSLDroidDbAdapter.KEY_ROWID, rowId);
	}

	@Override
	protected void onPause() {
		super.onPause();
		//saveState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		populateFields();
	}

	private void saveState() {
		String sName = name.getText().toString();
		int sLocalport = 0;
		try{
			sLocalport = Integer.parseInt(localport.getText().toString());
		} catch (NumberFormatException e){
		}
		String sRemotehost = remotehost.getText().toString();
		int sRemoteport = 0;
		try{
			sRemoteport = Integer.parseInt(remoteport.getText().toString());
		} catch (NumberFormatException e){
		} 
		String sPkcsfile = pkcsfile.getText().toString();
		String sPkcspass = pkcspass.getText().toString();
		
		//make sure that we have all of our values correctly set
		if (sName.length() == 0) {
			return;
		}
		if (sLocalport == 0) {
			return;
		}
		if (sRemotehost.length() == 0) {
			return;
		}
		if (sRemoteport == 0) {
			return;
		}
		if (sPkcsfile.length() == 0) {
			return;
		}
		
		if (rowId == null) {
			long id = dbHelper.createTunnel(sName, sLocalport, sRemotehost, sRemoteport, sPkcsfile, sPkcspass);
			if (id > 0) {
				rowId = id;
			}
		} else {
			dbHelper.updateTunnel(rowId, sName, sLocalport, sRemotehost, sRemoteport, sPkcsfile, sPkcspass);
		}
		Log.d("SSLDroid", "Saving settings...");
		
		//restart the service
		stopService(new Intent(this, SSLDroid.class));
		startService(new Intent(this, SSLDroid.class));
		Log.d("SSLDroid", "Restarting service after settings save...");
		
	}
}

