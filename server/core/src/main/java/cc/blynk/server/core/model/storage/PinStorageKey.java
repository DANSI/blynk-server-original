package cc.blynk.server.core.model.storage;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonValue;

import static cc.blynk.server.core.model.widgets.AppSyncWidget.SYNC_DEFAULT_MESSAGE_ID;
import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.prependDashIdAndDeviceId;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.11.16.
 */
public class PinStorageKey {

    public final int deviceId;

    final byte pin;

    private final char pinTypeChar;

    public PinStorageKey(int deviceId, PinType pinType, byte pin) {
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

    public StringMessage toStringMessage(int dashId, String value) {
        return toStringMessage(dashId, value, APP_SYNC);
    }

    StringMessage toStringMessage(int dashId, String value, short cmdType) {
        String body = prependDashIdAndDeviceId(dashId, deviceId, makeHardwareBody(value));
        return makeUTF8StringMessage(cmdType, SYNC_DEFAULT_MESSAGE_ID, body);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PinStorageKey)) {
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
        return "" + deviceId + StringUtils.DEVICE_SEPARATOR + pinTypeChar + pin;
    }
}
