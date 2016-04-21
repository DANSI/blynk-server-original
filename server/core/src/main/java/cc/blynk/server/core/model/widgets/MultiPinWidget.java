package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.enums.PinType;

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
    public void updateIfSame(byte pin, PinType type, String[] values) {
        if (pins != null) {
            for (int i = 0; i < pins.length; i++) {
                if (pins[i].isSame(pin, type)) {
                    pins[i].value = (values.length > 1 ? values[i] : values[0]);
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
    public String makeHardwareBody() {
        if (pins != null && pins[0].notEmpty()) {
            StringBuilder sb = new StringBuilder(pins[0].makeHardwareBody());
            for (int i = 1; i < pins.length; i++) {
                sb.append(BODY_SEPARATOR).append(pins[i].value);
            }
            return sb.toString();
        } else {
            return null;
        }
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
