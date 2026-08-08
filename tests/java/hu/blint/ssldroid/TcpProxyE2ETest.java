package hu.blint.ssldroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * End-to-end tests for the SSLDroid tunnel. Each test wires up the real
 * production {@link TcpProxy} in front of a real {@link TlsTestBackend} and
 * drives it with a plain-text client socket, asserting on the bytes that make
 * the full round trip:
 *
 * <pre>
 *   plain client --(cleartext)--&gt; SSLDroid TcpProxy --(TLS)--&gt; TlsTestBackend
 * </pre>
 *
 * Robolectric is used only so the production classes' {@code android.util.Log}
 * calls resolve at runtime; the networking and TLS are real.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TcpProxyE2ETest {

    private TlsTestBackend backend;
    private TcpProxy proxy;

    @Before
    public void setUp() throws Exception {
        backend = new TlsTestBackend();
    }

    @After
    public void tearDown() {
        if (proxy != null) {
            proxy.stop();
        }
        if (backend != null) {
            backend.close();
        }
    }

    @Test
    public void tunnelsCleartextClientToTlsBackendWithoutCaPinning() throws Exception {
        int listenPort = reserveFreePort();
        // Empty CA file => the proxy trusts any server certificate.
        proxy = new TcpProxy("no-pin", listenPort, "127.0.0.1", backend.getPort(),
                "", "", "", false);
        proxy.serve();

        String response = roundTrip(listenPort, "HELLO");
        assertEquals("PONG:HELLO", response);
    }

    @Test
    public void tunnelsWhenServerCertificateMatchesConfiguredCa() throws Exception {
        File caFile = resourceToTempFile("/hu/blint/ssldroid/server-cert.pem", "server-cert", ".pem");
        int listenPort = reserveFreePort();
        proxy = new TcpProxy("good-pin", listenPort, "127.0.0.1", backend.getPort(),
                "", "", caFile.getAbsolutePath(), false);
        proxy.serve();

        String response = roundTrip(listenPort, "PING");
        assertEquals("PONG:PING", response);
    }

    @Test
    public void refusesToTunnelWhenServerCertificateDoesNotMatchConfiguredCa() throws Exception {
        File caFile = resourceToTempFile("/hu/blint/ssldroid/other-cert.pem", "other-cert", ".pem");
        int listenPort = reserveFreePort();
        proxy = new TcpProxy("bad-pin", listenPort, "127.0.0.1", backend.getPort(),
                "", "", caFile.getAbsolutePath(), false);
        proxy.serve();

        // The backend cert is not signed by the configured CA, so no plaintext
        // must ever reach the client. We should see no "PONG" — either an EOF
        // or a read timeout, never a valid tunnelled response.
        String response = readWithTimeout(listenPort, "SECRET", 3000);
        assertFalse("wrong-CA tunnel must not deliver backend data, got: " + response,
                response.contains("PONG"));
    }

    // --- helpers -----------------------------------------------------------

    /** Performs a single request/response round trip through the proxy. */
    private static String roundTrip(int proxyPort, String request) throws IOException {
        try (Socket client = connectWithRetry(proxyPort, 5000)) {
            client.setSoTimeout(5000);
            OutputStream out = client.getOutputStream();
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();
            return readAll(client.getInputStream());
        }
    }

    /**
     * Sends a request and reads whatever comes back within {@code timeoutMs},
     * returning an empty string if the read times out or hits EOF immediately.
     */
    private static String readWithTimeout(int proxyPort, String request, int timeoutMs) throws IOException {
        try (Socket client = connectWithRetry(proxyPort, 5000)) {
            client.setSoTimeout(timeoutMs);
            OutputStream out = client.getOutputStream();
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();
            try {
                return readAll(client.getInputStream());
            } catch (SocketTimeoutException e) {
                return "";
            }
        }
    }

    private static String readAll(InputStream in) throws IOException {
        byte[] buf = new byte[4096];
        int n = in.read(buf);
        if (n <= 0) {
            return "";
        }
        return new String(buf, 0, n, StandardCharsets.UTF_8);
    }

    /**
     * The proxy binds its listening socket asynchronously on its own thread, so
     * the client may briefly beat it to the port. Retry connecting until the
     * proxy is up or the deadline passes.
     */
    private static Socket connectWithRetry(int port, long deadlineMs) throws IOException {
        long deadline = System.currentTimeMillis() + deadlineMs;
        IOException last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                return new Socket(InetAddress.getByName("127.0.0.1"), port);
            } catch (IOException e) {
                last = e;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IOException("Could not connect to proxy on port " + port);
    }

    /** Reserves (and releases) a free loopback TCP port for the proxy to bind. */
    private static int reserveFreePort() throws IOException {
        try (ServerSocket ss = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            return ss.getLocalPort();
        }
    }

    private static File resourceToTempFile(String resource, String prefix, String suffix) throws IOException {
        File tmp = File.createTempFile(prefix, suffix);
        tmp.deleteOnExit();
        try (InputStream in = TcpProxyE2ETest.class.getResourceAsStream(resource);
             OutputStream out = new FileOutputStream(tmp)) {
            if (in == null) {
                fail("Missing test resource: " + resource);
            }
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
        return tmp;
    }
}
