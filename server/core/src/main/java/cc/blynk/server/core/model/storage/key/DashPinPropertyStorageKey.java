package cc.blynk.server.core.model.storage.key;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.utils.StringUtils;

import java.util.Objects;

import static cc.blynk.server.core.model.DataStream.makePropertyHardwareBody;
import static cc.blynk.server.core.protocol.enums.Command.SET_WIDGET_PROPERTY;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.06.17.
 */
public final class DashPinPropertyStorageKey extends DashPinStorageKey {

    private final WidgetProperty property;

    private DashPinPropertyStorageKey(int dashId, int deviceId, char pinTypeChar, short pin, WidgetProperty property) {
        super(dashId, deviceId, pinTypeChar, pin);
        this.property = property;
    }

    public DashPinPropertyStorageKey(int dashId, int deviceId, PinType pinType, short pin, WidgetProperty property) {
        super(dashId, deviceId, pinType, pin);
        this.property = property;
    }

    public DashPinPropertyStorageKey(int dashId, PinPropertyStorageKey pinPropertyStorageKey) {
        this(dashId, pinPropertyStorageKey.deviceId,
                pinPropertyStorageKey.pinTypeChar,
                pinPropertyStorageKey.pin,
                pinPropertyStorageKey.property);
    }

    @Override
    public String makeHardwareBody(String value) {
        return makePropertyHardwareBody(pin, property, value);
    }

    @Override
    public short getCmdType() {
        return SET_WIDGET_PROPERTY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        DashPinPropertyStorageKey that = (DashPinPropertyStorageKey) o;
        return property == that.property;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), property);
    }

    @Override
    public String toString() {
        return super.toString() + StringUtils.DEVICE_SEPARATOR + property.label;
    }
}
