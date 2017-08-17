package cc.blynk.utils.serialization;

import cc.blynk.server.core.model.device.HardwareInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * User who see shared dashboard should not see authentification data of original user
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 08.12.16.
 */
public abstract class DashboardMixIn {

    @JsonIgnore
    public String sharedToken;

    @JsonIgnore
    public HardwareInfo hardwareInfo;

    @JsonIgnore
    public int parentId;

    @JsonIgnore
    public boolean isPreview;

}
