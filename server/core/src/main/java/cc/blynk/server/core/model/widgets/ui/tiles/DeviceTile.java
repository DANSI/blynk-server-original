package cc.blynk.server.core.model.widgets.ui.tiles;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.10.17.
 */
public class DeviceTile {

    public final int deviceId;

    public final long templateId;

    @JsonProperty("pin")
    public final DataStream dataStream;

    @JsonCreator
    public DeviceTile(@JsonProperty("deviceId") int deviceId,
                      @JsonProperty("templateId") long templateId,
                      @JsonProperty("pin") DataStream dataStream) {
        this.deviceId = deviceId;
        this.templateId = templateId;
        this.dataStream = dataStream;
    }

    public boolean updateIfSame(int deviceId, byte pin, PinType pinType, String value) {
        if (this.deviceId == deviceId && dataStream != null && dataStream.isSame(pin, pinType)) {
            this.dataStream.value = value;
            return true;
        }
        return false;
    }
}
