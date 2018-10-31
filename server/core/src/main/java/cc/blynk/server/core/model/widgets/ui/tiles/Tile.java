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
public class Tile {

    public final int deviceId;

    public final long templateId;

    public final String iconName;

    @JsonProperty("pin")
    public final DataStream dataStream;

    private transient long lastRequestTS;

    @JsonCreator
    public Tile(@JsonProperty("deviceId") int deviceId,
                @JsonProperty("templateId") long templateId,
                @JsonProperty("iconName") String iconName,
                @JsonProperty("pin") DataStream dataStream) {
        this.deviceId = deviceId;
        this.templateId = templateId;
        this.iconName = iconName;
        this.dataStream = dataStream;
    }

    public boolean isSame(int deviceId, short pin, PinType pinType) {
        return this.deviceId == deviceId && dataStream != null && dataStream.isSame(pin, pinType);
    }

    public boolean updateIfSame(int deviceId, DataStream dataStream) {
        if (dataStream != null) {
            return updateIfSame(deviceId, dataStream.pin, dataStream.pinType, dataStream.value);
        }
        return false;
    }

    public boolean updateIfSame(int deviceId, short pin, PinType pinType, String value) {
        if (isSame(deviceId, pin, pinType)) {
            this.dataStream.value = value;
            return true;
        }
        return false;
    }

    public boolean isValidDataStream() {
        return dataStream != null && dataStream.isValid();
    }

    public void erase() {
        if (dataStream != null) {
            dataStream.value = null;
        }
    }

    public boolean isTicked(long now) {
        //todo 1000 is hardcoded for now
        if (now >= lastRequestTS + 1000) {
            this.lastRequestTS = now;
            return true;
        }
        return false;
    }
}
