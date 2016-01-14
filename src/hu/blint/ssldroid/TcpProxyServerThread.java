package hu.blint.ssldroid;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.util.Log;

public class TcpProxyServerThread extends Thread {

    String tunnelName;
    int listenPort;
    String tunnelHost;
    int tunnelPort;
    String keyFile, keyPass, caFile;
    Relay inRelay, outRelay;
    ServerSocket ss = null;
    int sessionid = 0;
    private SSLSocketFactory sslSocketFactory;
    private X509Certificate caCert;

    public TcpProxyServerThread(String tunnelName, int listenPort, String tunnelHost, int tunnelPort, String keyFile, String keyPass, String caFile) {
        this.tunnelName = tunnelName;
        this.listenPort = listenPort;
        this.tunnelHost = tunnelHost;
        this.tunnelPort = tunnelPort;
        this.keyFile = keyFile;
        this.keyPass = keyPass;
        this.caFile = caFile;

        // Loading the CA cert
        if (caFile != null && !caFile.isEmpty()) {
            InputStream inStream = null;
            try {
                inStream = new FileInputStream(this.caFile);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                caCert = (X509Certificate) cf.generateCertificate(inStream);
            } catch (Exception ex) {
                //FIXME
            } finally {
                try {
                    if (inStream != null)
                        inStream.close();
                } catch (IOException ex) { }
            }
        }
    }

    // Create a trust manager that does not validate certificate chains
    // TODO: handle this somehow properly (popup if cert is untrusted?)
    // TODO: cacert + crl should be configurable
    /*TrustManager[] trustAllCerts = new TrustManager[] {
    new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        public void checkClientTrusted(
        java.security.cert.X509Certificate[] certs, String authType) {
        }
        public void checkServerTrusted(
        java.security.cert.X509Certificate[] certs, String authType) {
        }
    }
    };*/

    // FIXME: https://stackoverflow.com/questions/6629473/validate-x-509-certificate-agains-concrete-ca-java
    TrustManager[] trustCaCert = new TrustManager[] {
    new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
        }
        public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType) throws CertificateException {

            if (caFile == null || caFile.isEmpty()) //No CA file - trust all
                return;

            if (certs == null || certs.length == 0) {
                throw new IllegalArgumentException("null or zero-length certificate chain");
            }

            if (authType == null || authType.length() == 0) {
                throw new IllegalArgumentException("null or zero-length authentication type");
            }

            if (caCert == null) { //CA file specified, but no CA cert loaded
                throw new CertificateException("Invalid CA cert");
            }

            //Check if top-most cert is our CA's
            if(!certs[0].equals(caCert)){
                try
                {   //Not our CA's. Check if it has been signed by our CA
                    certs[0].verify(caCert.getPublicKey());
                }
                catch(Exception e){
                    throw new CertificateException("Certificate not trusted",e);
                }
            }

            //If we end here certificate is trusted. Check if any cert in the chain has expired.
            try{
                for (X509Certificate cert : certs) {
                    cert.checkValidity();
                }
            }
            catch(Exception e){
                throw new CertificateException("Certificate not trusted. It has expired",e);
            }
        }
    }
    };



    public final SSLSocketFactory getSocketFactory(String pkcsFile,
            String pwd, int sessionid) {
        if (sslSocketFactory == null) {
            try {
                KeyManagerFactory keyManagerFactory;
                if (pkcsFile != null && !pkcsFile.isEmpty()) {
                    keyManagerFactory = KeyManagerFactory.getInstance("X509");
                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    keyStore.load(new FileInputStream(pkcsFile), pwd.toCharArray());
                    keyManagerFactory.init(keyStore, pwd.toCharArray());
                } else {
                    keyManagerFactory = null;
                }
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(), trustCaCert,
                             new SecureRandom());
                sslSocketFactory = context.getSocketFactory();
            } catch (FileNotFoundException e) {
                Log.d("SSLDroid", tunnelName+"/"+sessionid+": Error loading the client certificate file:"
                      + e.toString());
            } catch (KeyManagementException e) {
                Log.d("SSLDroid", tunnelName+"/"+sessionid+": No SSL algorithm support: " + e.toString());
            } catch (NoSuchAlgorithmException e) {
                Log.d("SSLDroid", tunnelName+"/"+sessionid+": No common SSL algorithm found: " + e.toString());
            } catch (KeyStoreException e) {
                Log.d("SSLDroid", tunnelName+"/"+sessionid+": Error setting up keystore:" + e.toString());
            } catch (java.security.cert.CertificateException e) {
                Log.d("SSLDroid", tunnelName+"/"+sessionid+": Error loading the client certificate:" + e.toString());
            } catch (IOException e) {
                Log.d("SSLDroid", tunnelName+"/"+sessionid+": Error loading the client certificate file:" + e.toString());
            } catch (UnrecoverableKeyException e) {
                Log.d("SSLDroid", tunnelName+"/"+sessionid+": Error loading the client certificate:" + e.toString());
            }
        }
        return sslSocketFactory;
    }

    public void run() {
        try {
            ss = new ServerSocket(listenPort, 50, InetAddress.getLocalHost());
            Log.d("SSLDroid", "Listening for connections on "+InetAddress.getLocalHost().getHostAddress()+":"+
                  + this.listenPort + " ...");
        } catch (Exception e) {
            Log.d("SSLDroid", "Error setting up listening socket: " + e.toString());
            return;
        }
        while (true) {
            try {
                Thread fromBrowserToServer = null;
                Thread fromServerToBrowser = null;

                if (isInterrupted()) {
                    Log.d("SSLDroid", tunnelName+"/"+sessionid+": Interrupted server thread, closing sockets...");
                    ss.close();
                    return;
                }
                // accept the connection from my client
                Socket sc = null;
                try {
                    sc = ss.accept();
                    sessionid++;
                } catch (SocketException e) {
                    Log.d("SSLDroid", "Accept failure: " + e.toString());
                }

                Socket st = null;
                try {
                    final SSLSocketFactory sf = getSocketFactory(this.keyFile, this.keyPass, this.sessionid);
                    st = (SSLSocket) sf.createSocket(this.tunnelHost, this.tunnelPort);
                    setSNIHost(sf, (SSLSocket) st, this.tunnelHost);
                    ((SSLSocket) st).startHandshake();
                } catch (IOException e) {
                    Log.d("SSLDroid", tunnelName+"/"+sessionid+": SSL failure: " + e.toString());
                    return;
                }
                catch (Exception e) {
                    Log.d("SSLDroid", tunnelName+"/"+sessionid+": SSL failure: " + e.toString());
                    if (sc != null)
                      {
                        sc.close();
                      }
                    return;
                }

                if (sc == null || st == null) {
                    Log.d("SSLDroid", tunnelName+"/"+sessionid+": Trying socket operation on a null socket, returning");
                    return;
                }
                Log.d("SSLDroid", tunnelName+"/"+sessionid+": Tunnelling port "
                      + listenPort + " to port "
                      + tunnelPort + " on host "
                      + tunnelHost + " ...");

                // relay the stuff through
                fromBrowserToServer = new Relay(
                    this, sc.getInputStream(), st.getOutputStream(), "client", sessionid);
                fromServerToBrowser = new Relay(
                    this, st.getInputStream(), sc.getOutputStream(), "server", sessionid);

                fromBrowserToServer.start();
                fromServerToBrowser.start();

            } catch (IOException ee) {
                Log.d("SSLDroid", tunnelName+"/"+sessionid+": Ouch: " + ee.toString());
            }
        }
    }

    private void setSNIHost(final SSLSocketFactory factory, final SSLSocket socket, final String hostname) {
        if (factory instanceof android.net.SSLCertificateSocketFactory && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            ((android.net.SSLCertificateSocketFactory)factory).setHostname(socket, hostname);
        } else {
            try {
                socket.getClass().getMethod("setHostname", String.class).invoke(socket, hostname);
            } catch (Throwable e) {
                // ignore any error, we just can't set the hostname...
            }
        }
    }
};

