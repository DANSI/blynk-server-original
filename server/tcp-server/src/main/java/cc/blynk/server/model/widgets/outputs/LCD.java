package cc.blynk.server.model.widgets.outputs;

import cc.blynk.server.model.HardwareBody;
import cc.blynk.server.model.Pin;
import cc.blynk.server.model.enums.PinType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class LCD extends FrequencyWidget {

    public boolean advancedMode;

    public String textFormatLine1;

    public String textFormatLine2;

    public boolean textLight;

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
    public boolean isSame(byte pinIn, PinType type) {
        if (pins != null) {
            for (Pin pin : pins) {
                if (pin.isSame(pinIn, type)) {
                    return true;
                }
            }
        }
        return false;
    }
}
