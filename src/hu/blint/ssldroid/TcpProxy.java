package hu.blint.ssldroid;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.sql.Timestamp;
import java.util.Date;

import android.util.Log;

/**
 * This is a modified version of the TcpTunnelGui utility borrowed from the
 * xml.apache.org project.
 */
public class TcpProxy {
	int listenPort;
	String tunnelHost;
	int tunnelPort;
	String keyFile, keyPass;
	Thread server = null;
	ServerSocket ss = null;

	public TcpProxy() {
	}

	public void createNotification(String title, String text) {
		try {
			FileWriter outFile = new FileWriter("/mnt/sdcard/ssldroid.txt");
			PrintWriter out = new PrintWriter(outFile);
			Date date= new Date();
            
			out.println(new Timestamp(date.getTime())+" "+title+" "+text);
			out.close();
		} catch (IOException e){
			return;
		}
	}

	public void serve(int listenPort, String tunnelHost, int tunnelPort, String keyFile, String keyPass) throws IOException {
		try {
			ss = new ServerSocket(listenPort);
			Log.d("SSLDroid", "Listening for connections on port "
					+ listenPort + " ...");
		} catch (Exception e) {
			Log.d("SSLDroid", "Error setting up listening socket: " + e.toString());
			System.exit(1);
		}
		server = new TcpProxyServerThread(ss, listenPort, tunnelHost, tunnelPort, keyFile, keyPass);
		server.start();
	}

	public void stop() {
		if (server != null){
			try {
				ss.close();
				server.interrupt();
			} catch (Exception e) {
				Log.d("SSLDroid", "Interrupt failure: " + e.toString());
				createNotification(e.getMessage(), "Ouch: "+e.toString());;
			}
		}
		Log.d("SSLDroid", "Stopping service");
	}


}
