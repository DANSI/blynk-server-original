package cc.blynk.server.application.handlers.main.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.utils.StringUtils.split3;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.17.
 */
public final class Version {

    private static final Logger log = LogManager.getLogger(Version.class);

    public static final Version UNKNOWN_VERSION = new Version("", null);

    public final OsType osType;
    public final int versionSingleNumber;

    public Version(String osString, String versionString) {
        this.osType = OsType.parse(osString);
        this.versionSingleNumber = Version.parseToSingleInt(versionString);
    }

    /**
     * Expecting only strings like "1.2.2"
     */
    private static int parseToSingleInt(String version) {
        if (version != null) {
            String[] parts = split3('.', version);
            try {
                return parse(parts);
            } catch (Exception e) {
                log.warn("Error parsing app versionSingleNumber {}. Reason : {}.", version, e.getMessage());
            }
        }
        return 0;
    }

    private static int parse(String[] parts) {
        return Integer.parseInt(parts[0]) * 10_000
                + Integer.parseInt(parts[1]) * 100
                + Integer.parseInt(parts[2]);
    }

    public boolean largerThan(Version version) {
        return this.versionSingleNumber > version.versionSingleNumber;
    }
}
