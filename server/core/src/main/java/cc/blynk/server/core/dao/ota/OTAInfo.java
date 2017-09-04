package cc.blynk.server.core.dao.ota;

import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.08.17.
 */
public class OTAInfo {

    final long initiatedAt;
    final String initiatedBy;
    final String pathToFirmware;
    final String build;
    final String projectName;

    OTAInfo(String initiatedBy, String pathToFirmware, String build, String projectName) {
        this.initiatedAt = System.currentTimeMillis();
        this.initiatedBy = initiatedBy;
        this.pathToFirmware = pathToFirmware;
        this.build = build;
        this.projectName = projectName;
    }

    public String makeHardwareBody(String serverHostUrl) {
        return makeHardwareBody(serverHostUrl, pathToFirmware);
    }

    public static String makeHardwareBody(String serverHostUrl, String pathToFirmware) {
        return "ota" + BODY_SEPARATOR + serverHostUrl + pathToFirmware;
    }

    public boolean matches(String dashName) {
        return projectName == null || projectName.equalsIgnoreCase(dashName);
    }

    @Override
    public String toString() {
        return "OTAInfo{"
                + "initiatedAt=" + initiatedAt
                + ", initiatedBy='" + initiatedBy + '\''
                + ", pathToFirmware='" + pathToFirmware + '\''
                + ", build='" + build + '\''
                + ", projectName='" + projectName + '\''
                + '}';
    }
}
