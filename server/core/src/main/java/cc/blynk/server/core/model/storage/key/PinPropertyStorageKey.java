package cc.blynk.server.core.model.storage.key;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.utils.StringUtils;

import static cc.blynk.server.core.model.DataStream.makePropertyHardwareBody;
import static cc.blynk.server.core.protocol.enums.Command.SET_WIDGET_PROPERTY;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.06.17.
 */
@Deprecated
public final class PinPropertyStorageKey extends PinStorageKey {

    public final WidgetProperty property;

    public PinPropertyStorageKey(int deviceId, PinType pinType, short pin, WidgetProperty property) {
        super(deviceId, pinType, pin);
        this.property = property;
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
        if (!(o instanceof PinPropertyStorageKey)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        PinPropertyStorageKey that = (PinPropertyStorageKey) o;

        return property == that.property;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (property != null ? property.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + StringUtils.DEVICE_SEPARATOR + property.label;
    }
}
