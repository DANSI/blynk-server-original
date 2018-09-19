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

    public static final Version UNKNOWN_VERSION = new Version(OsType.OTHER, 0);

    public final OsType osType;
    final int versionSingleNumber;

    public Version(OsType osType, int version) {
        this.osType = osType;
        this.versionSingleNumber = version;
    }

    public Version(String osString, String versionString) {
        this(OsType.parse(osString), parseToSingleInt(versionString));
    }

    /**
     * Expecting only strings like "1.2.2"
     */
    private static int parseToSingleInt(String version) {
        try {
            var parts = split3('.', version);
            return parse(parts);
        } catch (Exception e) {
            log.debug("Error parsing app versionSingleNumber {}. Reason : {}.", version, e.getMessage());
        }
        return 0;
    }

    private static int parse(String[] parts) {
        return Integer.parseInt(parts[0]) * 10_000
                + Integer.parseInt(parts[1]) * 100
                + Integer.parseInt(parts[2]);
    }

    boolean largerOrEqualThan(int version) {
        return this.versionSingleNumber >= version;
    }

    //this method should be changed to notify users that they use outdated app.
    //done mostly for some changes that cannot be used on old apps.
    //not used right now.
    boolean isOutdated() {
        //hardcoded value for tests
        return versionSingleNumber == 10101;
    }

    @Override
    public String toString() {
        return osType.label + "-" + versionSingleNumber;
    }
}
