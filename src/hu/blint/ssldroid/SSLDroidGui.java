package hu.blint.ssldroid;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import hu.blint.ssldroid.db.SSLDroidDbAdapter;

public class SSLDroidGui extends ListActivity {
    private SSLDroidDbAdapter dbHelper;
    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;
    private static final int DELETE_ID = Menu.FIRST + 1;
    private Cursor cursor;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tunnel_list);
        this.getListView().setDividerHeight(2);
        dbHelper = new SSLDroidDbAdapter(this);
        dbHelper.open();
        fillData();
        registerForContextMenu(getListView());
    }

    // Create the menu based on the XML defintion
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    // Reaction to the menu selection
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case R.id.addtunnel:
            createTunnel();
            return true;
        case R.id.stopservice:
            Log.d("SSLDroid", "Stopping service");
            stopService(new Intent(this, SSLDroid.class));
            return true;
        case R.id.stopserviceforgood:
            Log.d("SSLDroid", "Stopping service until explicitly started");
            dbHelper.setStopStatus();
            stopService(new Intent(this, SSLDroid.class));
            return true;
        case R.id.startservice:
            Log.d("SSLDroid", "Starting service");
            dbHelper.delStopStatus();
            startService(new Intent(this, SSLDroid.class));
            return true;
        case R.id.readlogs:
            readLogs();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.addtunnel:
            createTunnel();
            return true;
        case R.id.stopservice:
            Log.d("SSLDroid", "Stopping service");
            stopService(new Intent(this, SSLDroid.class));
            return true;
        case R.id.stopserviceforgood:
            Log.d("SSLDroid", "Stopping service until explicitly started");
            dbHelper.setStopStatus();
            stopService(new Intent(this, SSLDroid.class));
            return true;
        case R.id.startservice:
            Log.d("SSLDroid", "Starting service");
            dbHelper.delStopStatus();
            startService(new Intent(this, SSLDroid.class));
            return true;
        case R.id.readlogs:
            readLogs();
            return true;
        //case R.id.provision:
        //    getProvisioning();
        //    return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case DELETE_ID:
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                                          .getMenuInfo();
            dbHelper.deleteTunnel(info.id);
            fillData();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void createTunnel() {
        Intent i = new Intent(this, SSLDroidTunnelDetails.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    private void readLogs() {
        Intent i = new Intent(this, SSLDroidReadLogs.class);
        startActivity(i);
    }

    private void getProvisioning() {
        Intent i = new Intent(this, SSLDroidProvisioning.class);
        startActivity(i);
    }
    
    // ListView and view (row) on which was clicked, position and
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, SSLDroidTunnelDetails.class);
        i.putExtra(SSLDroidDbAdapter.KEY_ROWID, id);
        // Activity returns an result if called with startActivityForResult
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    // Called with the result of the other activity
    // requestCode was the origin request code send to the activity
    // resultCode is the return code, 0 is everything is ok
    // intend can be use to get some data from the caller
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData();

    }

    private void fillData() {
        cursor = dbHelper.fetchAllTunnels();
        startManagingCursor(cursor);

        String[] from = new String[] { SSLDroidDbAdapter.KEY_NAME };
        int[] to = new int[] { R.id.text1 };

        // Now create an array adapter and set it to display using our row
        SimpleCursorAdapter tunnels = new SimpleCursorAdapter(this,
                R.layout.tunnel_list_item, cursor, from, to);
        setListAdapter(tunnels);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
    }
    
    @Override
    public void onDestroy (){
	cursor.close();
	dbHelper.close();
	super.onDestroy();
    }
    
}
