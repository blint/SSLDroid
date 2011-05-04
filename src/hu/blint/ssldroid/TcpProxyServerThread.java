package hu.blint.ssldroid;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;

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
	String keyFile, keyPass;
	Relay inRelay, outRelay;
	ServerSocket ss = null;
	int sessionid = 0;

	public TcpProxyServerThread(ServerSocket ss,String tunnelName, int listenPort, String tunnelHost, int tunnelPort, String keyFile, String keyPass) {
		this.tunnelName = tunnelName;
		this.listenPort = listenPort;
		this.tunnelHost = tunnelHost;
		this.tunnelPort = tunnelPort;
		this.keyFile = keyFile;
		this.keyPass = keyPass;
		this.ss = ss;
	}
	
	// Create a trust manager that does not validate certificate chains
	// TODO: handle this somehow properly (popup if cert is untrusted?)
	// TODO: cacert + crl should be configurable
	TrustManager[] trustAllCerts = new TrustManager[]{
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
	};	
	
	private static SSLSocketFactory sslSocketFactory;

	public final SSLSocketFactory getSocketFactory(String pkcsFile,
			String pwd, int sessionid) {
		if (sslSocketFactory == null) {
			try {
				KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
				KeyStore keyStore = KeyStore.getInstance("PKCS12");
				keyStore.load(new FileInputStream(pkcsFile), pwd.toCharArray());
				keyManagerFactory.init(keyStore, pwd.toCharArray());
				SSLContext context = SSLContext.getInstance("TLS");
				context.init(keyManagerFactory.getKeyManagers(), trustAllCerts,
						new SecureRandom());
				sslSocketFactory = (SSLSocketFactory) context.getSocketFactory();

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
		    
	public class Relay extends Thread {
		private InputStream in;
		private OutputStream out;
		private String side;
		private int sessionid;
		private final static int BUFSIZ = 4096;
		private byte buf[] = new byte[BUFSIZ];

		public Relay(InputStream in, OutputStream out, String side, int sessionid) {
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
						Log.d("SSLDroid", tunnelName+"/"+sessionid+": Interrupted "+side+" thread");
						try {
							in.close();
							out.close();
						} catch (IOException e) {
							Log.d("SSLDroid", tunnelName+"/"+sessionid+": "+e.toString());
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
				Log.d("SSLDroid", tunnelName+"/"+sessionid+": "+e.toString());
			} catch (IOException e) {
				Log.d("SSLDroid", tunnelName+"/"+sessionid+": "+e.toString());
			} finally {
				try {
					in.close();
					out.close();
				} catch (IOException e) {
					Log.d("SSLDroid", tunnelName+"/"+sessionid+": "+e.toString());
				}
			}
			Log.d("SSLDroid", tunnelName+"/"+sessionid+": Quitting "+side+"-side stream proxy...");
		}
	}
	
	public void run() {
		while (true) {			
			try {
				Thread fromBrowserToServer = null;
				Thread fromServerToBrowser = null;
				
				if (isInterrupted()){
					Log.d("SSLDroid", tunnelName+"/"+sessionid+": Interrupted server thread, closing sockets...");
					ss.close();
					if (fromBrowserToServer != null)
						fromBrowserToServer.notify();
					if (fromServerToBrowser != null)
						fromServerToBrowser.notify();
					return;
				}
				// accept the connection from my client
				Socket sc = null;
				try {
					sc = ss.accept();
					sessionid++;
				} catch (SocketException e){
					Log.d("SSLDroid", "Accept failure: " + e.toString());
				}
				
				Socket st = null;
				try {
					st = (SSLSocket) getSocketFactory(this.keyFile, this.keyPass, this.sessionid).createSocket(this.tunnelHost, this.tunnelPort);
					((SSLSocket) st).startHandshake();
				} catch (IOException e){
					
				}
				catch (Exception e) {
					Log.d("SSLDroid", tunnelName+"/"+sessionid+": SSL failure: " + e.toString());
					sc.close();
					return;
				}

				if (sc == null || st == null){
					Log.d("SSLDroid", tunnelName+"/"+sessionid+": Trying socket operation on a null socket, returning");
					return;
				}
				Log.d("SSLDroid", tunnelName+"/"+sessionid+": Tunnelling port "
						+ listenPort + " to port "
						+ tunnelPort + " on host "
						+ tunnelHost + " ...");
				
				// relay the stuff through
				fromBrowserToServer = new Relay(
						sc.getInputStream(), st.getOutputStream(), "client", sessionid);
				fromServerToBrowser = new Relay(
						st.getInputStream(), sc.getOutputStream(), "server", sessionid);

				fromBrowserToServer.start();
				fromServerToBrowser.start();

			} catch (IOException ee) {
				Log.d("SSLDroid", tunnelName+"/"+sessionid+": Ouch: " + ee.toString());
			}
		}
	}
};

