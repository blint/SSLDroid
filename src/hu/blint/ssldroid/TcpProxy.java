package hu.blint.ssldroid;

import java.io.IOException;
import android.util.Log;

/**
 * This is a modified version of the TcpTunnelGui utility borrowed from the
 * xml.apache.org project.
 */
public class TcpProxy {
    String tunnelName;
    int listenPort;
    String tunnelHost;
    int tunnelPort;
    String keyFile, keyPass;
    TcpProxyServerThread server = null;

    public TcpProxy(String tunnelName, int listenPort, String targetHost, int targetPort, String keyFile, String keyPass) {
        this.tunnelName = tunnelName;
        this.listenPort = listenPort;
        this.tunnelHost = targetHost;
        this.tunnelPort = targetPort;
        this.keyFile = keyFile;
        this.keyPass = keyPass;
    }

    public void serve() throws IOException {
        server = new TcpProxyServerThread(this.tunnelName, this.listenPort, this.tunnelHost,
                                          this.tunnelPort, this.keyFile, this.keyPass);
        server.start();
    }

    public void stop() {
        if (server != null) {
            try {
                //close the server socket and interrupt the server thread
                server.ss.close();
                server.interrupt();
            } catch (Exception e) {
                Log.d("SSLDroid", "Interrupt failure: " + e.toString());
            }
        }
        Log.d("SSLDroid", "Stopping tunnel "+this.listenPort+":"+this.tunnelHost+":"+this.tunnelPort);
    }

    //if the listening socket is still active, we're alive
    public boolean isAlive() {
        return server.ss.isBound();
    }

}
