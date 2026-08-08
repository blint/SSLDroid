package hu.blint.ssldroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link SSLDroidDbAdapter} backed by Robolectric's real SQLite.
 *
 * <p>The {@link #persistsCaCertFileAndSniFlag()} case is a regression test for
 * the bug where {@code createContentValues} silently dropped the
 * {@code cacertfile} and {@code usesni} columns, which (a) lost the CA-pinning
 * and SNI settings and (b) failed the {@code usesni NOT NULL} constraint on a
 * fresh database, so tunnels could not be saved at all.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SSLDroidDbAdapterTest {

    private SSLDroidDbAdapter db;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        db = new SSLDroidDbAdapter(context);
        db.open();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void createTunnelReturnsPositiveRowId() {
        long id = db.createTunnel("t1", 1234, "example.com", 443,
                "", "", "", 1);
        assertTrue("createTunnel should return a valid row id, got " + id, id > 0);
    }

    @Test
    public void persistsCaCertFileAndSniFlag() {
        long id = db.createTunnel("pinned", 1234, "example.com", 443,
                "/sdcard/client.p12", "secret", "/sdcard/ca.pem", 0);
        assertTrue("createTunnel should succeed even with usesni=0", id > 0);

        Cursor c = db.fetchTunnel(id);
        try {
            assertEquals("pinned",
                    c.getString(c.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_NAME)));
            assertEquals("example.com",
                    c.getString(c.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_REMOTEHOST)));
            assertEquals(443,
                    c.getInt(c.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_REMOTEPORT)));
            assertEquals("CA cert file must be persisted", "/sdcard/ca.pem",
                    c.getString(c.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_CACERTFILE)));
            assertEquals("SNI flag must be persisted", 0,
                    c.getInt(c.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_USE_SNI)));
        } finally {
            c.close();
        }
    }

    @Test
    public void updateTunnelChangesStoredValues() {
        long id = db.createTunnel("orig", 1000, "a.example", 443,
                "", "", "", 1);
        db.updateTunnel(id, "renamed", 2000, "b.example", 8443,
                "", "", "/sdcard/new-ca.pem", 0);

        Cursor c = db.fetchTunnel(id);
        try {
            assertEquals("renamed",
                    c.getString(c.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_NAME)));
            assertEquals(2000,
                    c.getInt(c.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_LOCALPORT)));
            assertEquals("b.example",
                    c.getString(c.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_REMOTEHOST)));
            assertEquals("/sdcard/new-ca.pem",
                    c.getString(c.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_CACERTFILE)));
            assertEquals(0,
                    c.getInt(c.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_USE_SNI)));
        } finally {
            c.close();
        }
    }

    @Test
    public void deleteTunnelRemovesRow() {
        long id = db.createTunnel("todelete", 1234, "example.com", 443,
                "", "", "", 1);
        db.deleteTunnel(id);
        Cursor all = db.fetchAllTunnels();
        try {
            assertEquals(0, all.getCount());
        } finally {
            all.close();
        }
    }

    @Test
    public void stopStatusLifecycle() {
        assertEquals(0, countAndClose(db.getStopStatus()));
        db.setStopStatus();
        assertEquals(1, countAndClose(db.getStopStatus()));
        // setting it again must remain idempotent
        db.setStopStatus();
        assertEquals(1, countAndClose(db.getStopStatus()));
        db.delStopStatus();
        assertEquals(0, countAndClose(db.getStopStatus()));
    }

    private static int countAndClose(Cursor c) {
        try {
            return c.getCount();
        } finally {
            c.close();
        }
    }
}
