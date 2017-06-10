package cc.blynk.server.core.model;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.utils.StringUtils;

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

    public String makeHardwareBody(String value) {
        return Pin.makeHardwareBody(pinTypeChar, pin, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PinStorageKey)) return false;

        PinStorageKey that = (PinStorageKey) o;

        if (deviceId != that.deviceId) return false;
        if (pin != that.pin) return false;
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
    public String toString() {
        return "" + deviceId + StringUtils.DEVICE_SEPARATOR + pinTypeChar + pin;
    }
}
