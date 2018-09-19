package cc.blynk.server.application.handlers.main.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.17.
 */
public class VersionTest {

    @Test
    public void testCorrectVersion() {
        Version version = new Version("iOS", "1.2.3");
        assertEquals(OsType.IOS, version.osType);
        assertEquals(10203, version.versionSingleNumber);
    }

    @Test
    public void wrongValues() {
        Version version = new Version("iOS", "RC13");
        assertEquals(OsType.IOS, version.osType);
        assertEquals(0, version.versionSingleNumber);
    }

    @Test
    public void wrongValues2() {
        assertEquals(OsType.OTHER, Version.UNKNOWN_VERSION.osType);
        assertEquals(0, Version.UNKNOWN_VERSION.versionSingleNumber);
    }

    @Test
    public void testToString() {
        Version version = new Version("iOS", "1.2.4");
        assertEquals("iOS-10204", version.toString());

        version = new Version("iOS", "1.1.1");
        assertEquals("iOS-10101", version.toString());

        version = Version.UNKNOWN_VERSION;
        assertEquals("unknown-0", version.toString());
    }
}
