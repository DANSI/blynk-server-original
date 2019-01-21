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
 * Created on 19.11.16.
 */
@Deprecated
public class PinStorageKey {

    public final int deviceId;

    public final short pin;

    public final char pinTypeChar;

    public PinStorageKey(int deviceId, PinType pinType, short pin) {
        this.deviceId = deviceId;
        this.pinTypeChar = pinType.pintTypeChar;
        this.pin = pin;
    }

    public boolean isSamePin(OnePinWidget onePinWidget) {
        return this.pin == onePinWidget.pin && this.pinTypeChar == onePinWidget.pinType.pintTypeChar;
    }

    public boolean isSamePin(MultiPinWidget multiPinWidget) {
        if (multiPinWidget.dataStreams == null) {
            return false;
        }
        for (var dataStream : multiPinWidget.dataStreams) {
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

        PinStorageKey that = (PinStorageKey) o;

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
        int result = deviceId;
        result = 31 * result + (int) pin;
        result = 31 * result + (int) pinTypeChar;
        return result;
    }

    @Override
    @JsonValue
    public String toString() {
        return "" + deviceId + DEVICE_SEPARATOR + pinTypeChar + pin;
    }
}
