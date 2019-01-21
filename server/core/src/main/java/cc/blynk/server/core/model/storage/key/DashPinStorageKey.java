package cc.blynk.server.core.model.storage.key;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import com.fasterxml.jackson.annotation.JsonValue;

import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.utils.StringUtils.DEVICE_SEPARATOR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.11.18.
 */
public class DashPinStorageKey {

    public final int dashId;

    public final int deviceId;

    public final short pin;

    public final char pinTypeChar;

    public DashPinStorageKey(int dashId, int deviceId, char pintTypeChar, short pin) {
        this.dashId = dashId;
        this.deviceId = deviceId;
        this.pinTypeChar = pintTypeChar;
        this.pin = pin;
    }

    public DashPinStorageKey(int dashId, int deviceId, PinType pinType, short pin) {
        this(dashId, deviceId, pinType.pintTypeChar, pin);
    }

    public DashPinStorageKey(int dashId, PinStorageKey pinStorageKey) {
        this(dashId, pinStorageKey.deviceId, pinStorageKey.pinTypeChar, pinStorageKey.pin);
    }

    public boolean isSame(int dashId, OnePinWidget onePinWidget) {
        return this.dashId == dashId
                && this.pin == onePinWidget.pin
                && this.pinTypeChar == onePinWidget.pinType.pintTypeChar;
    }

    public boolean isSame(int dashId, MultiPinWidget multiPinWidget) {
        if (multiPinWidget.dataStreams == null || this.dashId != dashId) {
            return false;
        }
        for (DataStream dataStream : multiPinWidget.dataStreams) {
            if (dataStream.isSame(this.pin, PinType.getPinType(this.pinTypeChar))) {
                return true;
            }
        }
        return false;
    }

    public String makeHardwareBody(String value) {
        return DataStream.makeHardwareBody(pinTypeChar, pin, value);
    }

    public short getCmdType() {
        return APP_SYNC;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DashPinStorageKey that = (DashPinStorageKey) o;

        if (dashId != that.dashId) {
            return false;
        }
        if (deviceId != that.deviceId) {
            return false;
        }
        if (pin != that.pin) {
            return false;
        }
        return pinTypeChar == that.pinTypeChar;
    }

    @Override
    public int hashCode() {
        int result = dashId;
        result = 31 * result + deviceId;
        result = 31 * result + (int) pin;
        result = 31 * result + (int) pinTypeChar;
        return result;
    }

    @Override
    @JsonValue
    public String toString() {
        return "" + dashId + DEVICE_SEPARATOR + deviceId + DEVICE_SEPARATOR + pinTypeChar + pin;
    }
}
