package hu.blint.ssldroid;

import java.io.IOException;
import java.net.ServerSocket;

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
				//close the server socket and interrupt the server thread
				ss.close();
				server.interrupt();
			} catch (Exception e) {
				Log.d("SSLDroid", "Interrupt failure: " + e.toString());
			}
		}
		Log.d("SSLDroid", "Stopping service");
	}

	//if the listening socket is still active, we're alive
	public boolean isAlive(){
		return ss.isBound();
	}

}
