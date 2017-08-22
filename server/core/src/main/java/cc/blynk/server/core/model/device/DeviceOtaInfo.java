package cc.blynk.server.core.model.device;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 17.08.17.
 */
public class DeviceOtaInfo {

    public final String OTAInitiatedBy;

    public final long OTAInitiatedAt;

    public final long OTAUpdateAt;

    @JsonCreator
    public DeviceOtaInfo(@JsonProperty("OTAInitiatedBy") String OTAInitiatedBy,
                         @JsonProperty("OTAInitiatedAt") long OTAInitiatedAt,
                         @JsonProperty("OTAUpdateAt") long OTAUpdateAt) {
        this.OTAInitiatedBy = OTAInitiatedBy;
        this.OTAInitiatedAt = OTAInitiatedAt;
        this.OTAUpdateAt = OTAUpdateAt;
    }

    public boolean isLastOtaUpdateOk() {
        return OTAUpdateAt >= OTAInitiatedAt;
    }

}
