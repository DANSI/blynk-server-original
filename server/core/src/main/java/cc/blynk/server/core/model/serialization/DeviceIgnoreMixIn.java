package cc.blynk.server.core.model.serialization;

import cc.blynk.server.core.model.device.DeviceOtaInfo;
import cc.blynk.server.core.model.device.HardwareInfo;
import cc.blynk.server.core.model.device.Status;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * User who see shared dashboard should not see authentification data of original user
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 08.12.16.
 */
public abstract class DeviceIgnoreMixIn {

    @JsonIgnore
    public String token;

    @JsonIgnore
    public Status status;

    @JsonIgnore
    public long disconnectTime;

    @JsonIgnore
    public long connectTime;

    @JsonIgnore
    public long firstConnectTime;

    @JsonIgnore
    public long dataReceivedAt;

    @JsonIgnore
    public String lastLoggedIP;

    @JsonIgnore
    public HardwareInfo hardwareInfo;

    @JsonIgnore
    public DeviceOtaInfo deviceOtaInfo;

}
