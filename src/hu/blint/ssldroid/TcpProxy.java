package hu.blint.ssldroid;

import android.util.Log;

/**
 * This is a modified version of the TcpTunnelGui utility borrowed from the
 * xml.apache.org project.
 */
class TcpProxy {
    private final String tunnelName;
    private final int listenPort;
    private final String tunnelHost;
    private final int tunnelPort;
    private final String keyFile;
    private final String keyPass;
    private final String caCertFile;
    private final boolean useSNI;
    private TcpProxyServerThread server = null;

    public TcpProxy(String tunnelName, int listenPort, String targetHost, int targetPort, String keyFile, String keyPass, String caCertFile, boolean useSNI) {
        this.tunnelName = tunnelName;
        this.listenPort = listenPort;
        this.tunnelHost = targetHost;
        this.tunnelPort = targetPort;
        this.keyFile = keyFile;
        this.keyPass = keyPass;
        this.caCertFile = caCertFile;
        this.useSNI = useSNI;
    }

    public void serve() {
        server = new TcpProxyServerThread(this.tunnelName, this.listenPort, this.tunnelHost,
                                          this.tunnelPort, this.keyFile, this.keyPass, 
                                          this.caCertFile, this.useSNI);
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
