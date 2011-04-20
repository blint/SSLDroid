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
	
	int listenPort;
	String tunnelHost;
	int tunnelPort;
	String keyFile, keyPass;
	Relay inRelay, outRelay;
	ServerSocket ss = null;

	public TcpProxyServerThread(ServerSocket ss, int listenPort, String tunnelHost, int tunnelPort, String keyFile, String keyPass) {
		this.listenPort = listenPort;
		this.tunnelHost = tunnelHost;
		this.tunnelPort = tunnelPort;
		this.keyFile = keyFile;
		this.keyPass = keyPass;
		this.ss = ss;
	}
	
	// Create a trust manager that does not validate certificate chains
	// TODO: handle this somehow properly (popup if cert is untrusted?)
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
			String pwd) {
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
				Log.d("SSLDroid", "Error loading the client certificate file:"
						+ e.toString());
			} catch (KeyManagementException e) {
				Log.d("SSLDroid", "No SSL algorithm support: " + e.toString());
			} catch (NoSuchAlgorithmException e) {
				Log.d("SSLDroid", "No common SSL algorithm found: " + e.toString());
			} catch (KeyStoreException e) {
				Log.d("SSLDroid", "Error setting up keystore:" + e.toString());
			} catch (java.security.cert.CertificateException e) {
				Log.d("SSLDroid", "Error loading the client certificate:" + e.toString());
			} catch (IOException e) {
				Log.d("SSLDroid", "Error loading the client certificate file:" + e.toString());
			} catch (UnrecoverableKeyException e) {
				Log.d("SSLDroid", "Error loading the client certificate:" + e.toString());
			}
		}
		return sslSocketFactory;
	}
		    
	public class Relay extends Thread {
		private InputStream in;
		private OutputStream out;
		private final static int BUFSIZ = 4096;
		private byte buf[] = new byte[BUFSIZ];

		public Relay(InputStream in, OutputStream out) {
			this.in = in;
			this.out = out;
		}

		public void run() {
			int n = 0;

			try {
				while ((n = in.read(buf)) > 0) {
					if (Thread.interrupted()) {
						// We've been interrupted: no more relaying
						Log.d("SSLDroid", "Interrupted thread");
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
				Log.d("SSLDroid", e.toString());
			} catch (IOException e) {
				Log.d("SSLDroid", e.toString());
			} finally {
				try {
					in.close();
					out.close();
				} catch (IOException e) {
					Log.d("SSLDroid", e.toString());
				}
			}
			Log.d("SSLDroid", "Quitting stream proxy...");
		}
	}
	
	public void run() {
		while (true) {			
			try {
				
				Thread fromBrowserToServer = null;
				Thread fromServerToBrowser = null;
				
				if (isInterrupted()){
					Log.d("SSLDroid", "Interrupted server thread, closing sockets...");
					ss.close();
					if (fromBrowserToServer != null)
						fromBrowserToServer.wait();
					if (fromServerToBrowser != null)
						fromServerToBrowser.wait();
					return;
				}
				// accept the connection from my client
				Socket sc = null;
				try {
					sc = ss.accept();
				} catch (SocketException e){
					Log.d("SSLDroid", "Accept failure: " + e.toString());
				}
				
				Socket st = null;
				
				try {
					st = (SSLSocket) getSocketFactory(keyFile, keyPass).createSocket(tunnelHost, tunnelPort);
					((SSLSocket) st).startHandshake();
				} catch (Exception e) {
					Log.d("SSLDroid", "SSL failure: " + e.toString());
					//Thread.sleep(10000);
					//continue;
					sc.close();
					return;
				}

				Log.d("SSLDroid", "Tunnelling port "
						+ listenPort + " to port "
						+ tunnelPort + " on host "
						+ tunnelHost + " ...");
				
				// relay the stuff thru
				fromBrowserToServer = new Relay(
						sc.getInputStream(), st.getOutputStream());
				fromServerToBrowser = new Relay(
						st.getInputStream(), sc.getOutputStream());

				fromBrowserToServer.start();
				fromServerToBrowser.start();

			} catch (Exception ee) {
				Log.d("SSLDroid", "Ouch: " + ee.getMessage());
			}
		}
	}
};

