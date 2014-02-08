package hu.blint.ssldroid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

import android.util.Log;

public class Relay extends Thread {
    /**
     * 
     */
    private final TcpProxyServerThread tcpProxyServerThread;
    private InputStream in;
    private OutputStream out;
    private String side;
    private int sessionid;
    private final static int BUFSIZ = 4096;
    private byte buf[] = new byte[BUFSIZ];

    public Relay(TcpProxyServerThread tcpProxyServerThread, InputStream in, OutputStream out, String side, int sessionid) {
        this.tcpProxyServerThread = tcpProxyServerThread;
        this.in = in;
        this.out = out;
        this.side = side;
        this.sessionid = sessionid;
    }

    public void run() {
        int n = 0;

        try {
            while ((n = in.read(buf)) > 0) {
                if (Thread.interrupted()) {
                    // We've been interrupted: no more relaying
                    Log.d("SSLDroid", this.tcpProxyServerThread.tunnelName+"/"+sessionid+": Interrupted "+side+" thread");
                    try {
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        Log.d("SSLDroid", this.tcpProxyServerThread.tunnelName+"/"+sessionid+": "+e.toString());
                    }
                    return;
                }
                out.write(buf, 0, n);
                out.flush();

                for (int i = 0; i < n; i++) {
                    if (buf[i] == 7)
                        buf[i] = '#';
                }
            }
        } catch (SocketException e) {
            Log.d("SSLDroid", this.tcpProxyServerThread.tunnelName+"/"+sessionid+": "+e.toString());
        } catch (IOException e) {
            Log.d("SSLDroid", this.tcpProxyServerThread.tunnelName+"/"+sessionid+": "+e.toString());
        } catch (InterruptedException e) {
            Log.d("SSLDroid", this.tcpProxyServerThread.tunnelName+"/"+sessionid+": "+e.toString());
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                Log.d("SSLDroid", this.tcpProxyServerThread.tunnelName+"/"+sessionid+": "+e.toString());
            }
        }
        Log.d("SSLDroid", this.tcpProxyServerThread.tunnelName+"/"+sessionid+": Quitting "+side+"-side stream proxy...");
    }
}