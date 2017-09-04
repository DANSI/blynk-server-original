package cc.blynk.server.core.dao.ota;

import java.util.Date;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.08.17.
 */
class OTAInfo {

    final long initiatedAt;
    final String initiatedBy;
    final String firmwareInitCommandBody;
    final String build;

    OTAInfo(long initiatedAt, String initiatedBy, String firmwareInitCommandBody, String build) {
        this.initiatedAt = initiatedAt;
        this.initiatedBy = initiatedBy;
        this.firmwareInitCommandBody = firmwareInitCommandBody;
        this.build = build;
    }

    @Override
    public String toString() {
        return "OTAInfo{"
                + "initiatedAt=" + new Date(initiatedAt)
                + ", initiatedBy='" + initiatedBy + '\''
                + ", firmwareInitCommandBody='" + firmwareInitCommandBody + '\''
                + ", build='" + build + '\''
                + '}';
    }
}
