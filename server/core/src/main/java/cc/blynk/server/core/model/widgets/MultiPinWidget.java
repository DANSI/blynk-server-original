package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.HardwareBody;
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
    public void updateIfSame(HardwareBody body) {
        if (pins != null) {
            for (int i = 0; i < pins.length; i++) {
                if (pins[i].isSame(body.pin, body.type)) {
                    pins[i].value = (body.value.length > 1 ? body.value[i] : body.value[0]);
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
    public String makeHardwareBody() {
        StringBuilder sb = new StringBuilder(pins[0].makeHardwareBody());
        for (int i = 1; i < pins.length; i++) {
            sb.append(BODY_SEPARATOR).append(pins[i].value);
        }
        return sb.toString();
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
