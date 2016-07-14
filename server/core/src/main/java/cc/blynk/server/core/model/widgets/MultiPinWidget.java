package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.enums.PinType;
import io.netty.channel.Channel;

import java.util.StringJoiner;

import static cc.blynk.utils.StringUtils.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.11.15.
 */
public abstract class MultiPinWidget extends Widget {

    public Pin[] pins;

    @Override
    public void updateIfSame(byte pinIn, PinType type, String value) {
        if (pins != null) {
            for (Pin pin : pins) {
                if (pin.isSame(pinIn, type)) {
                    pin.value = value;
                }
            }
        }
    }

    @Override
    public boolean isSame(byte pinIn, PinType pinType) {
        if (pins != null) {
            for (Pin pin : pins) {
                if (pin.isSame(pinIn, pinType)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public abstract void sendSyncOnActivate(Channel appChannel, int dashId);

    public abstract boolean isSplitMode();

    public String makeHardwareBody(byte pinIn, PinType pinType) {
        if (pins == null) {
            return null;
        }
        if (isSplitMode()) {
            for (Pin pin : pins) {
                if (pin.isSame(pinIn, pinType)) {
                    return pin.makeHardwareBody();
                }
            }
        } else {
            if (pins[0].notEmpty()) {
                StringBuilder sb = new StringBuilder(pins[0].makeHardwareBody());
                for (int i = 1; i < pins.length; i++) {
                    sb.append(BODY_SEPARATOR).append(pins[i].value);
                }
                return sb.toString();
            }
        }
        return null;
    }

    @Override
    public String getValue(byte pinIn, PinType pinType) {
        if (pins != null) {
            for (Pin pin : pins) {
                if (pin.isSame(pinIn, pinType)) {
                    return pin.value;
                }
            }
        }
        return null;
    }

    @Override
    public boolean hasValue(String searchValue) {
        if (pins != null) {
            for (Pin pin : pins) {
                if (searchValue.equals(pin.value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getJsonValue() {
        if (pins == null) {
            return "[]";
        }
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (Pin pin : pins) {
            if (pin.value == null) {
                sj.add("\"\"");
            } else {
                sj.add("\"" + pin.value + "\"");
            }
        }
        return sj.toString();
    }

}
