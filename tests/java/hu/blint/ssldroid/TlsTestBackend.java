package hu.blint.ssldroid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 * A tiny TLS "echo with prefix" server used as the far end of an SSLDroid
 * tunnel in the end-to-end tests. It listens on 127.0.0.1 using the bundled
 * self-signed {@code testserver.p12} keystore, reads a single request line and
 * writes back {@code "PONG:" + request}.
 *
 * <p>This is deliberately implemented with plain JSSE (no Android APIs) so it
 * can act as a real, independent peer for the production proxy code.</p>
 */
class TlsTestBackend implements Runnable {

    static final String KEYSTORE_RESOURCE = "/hu/blint/ssldroid/testserver.p12";
    static final char[] KEYSTORE_PASSWORD = "testpass".toCharArray();

    private final SSLServerSocket serverSocket;
    private volatile boolean running = true;
    private final Thread thread;

    TlsTestBackend() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = TlsTestBackend.class.getResourceAsStream(KEYSTORE_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing test keystore resource: " + KEYSTORE_RESOURCE);
            }
            keyStore.load(in, KEYSTORE_PASSWORD);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD);
        context.init(kmf.getKeyManagers(), null, null);

        SSLServerSocketFactory factory = context.getServerSocketFactory();
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        serverSocket = (SSLServerSocket) factory.createServerSocket(0, 10, loopback);

        thread = new Thread(this, "tls-test-backend");
        thread.setDaemon(true);
        thread.start();
    }

    int getPort() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void run() {
        while (running) {
            try (SSLSocket client = (SSLSocket) serverSocket.accept()) {
                InputStream in = client.getInputStream();
                OutputStream out = client.getOutputStream();
                byte[] buf = new byte[4096];
                int n = in.read(buf);
                if (n <= 0) {
                    continue;
                }
                String request = new String(buf, 0, n, "UTF-8").trim();
                out.write(("PONG:" + request).getBytes("UTF-8"));
                out.flush();
            } catch (IOException e) {
                // Socket closed on shutdown, or a handshake failure from a
                // negative test case. Either way keep serving until stopped.
                if (!running) {
                    return;
                }
            }
        }
    }

    void close() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // best effort
        }
        thread.interrupt();
    }
}
