package cc.blynk.server.model.widgets.outputs;

import cc.blynk.server.model.HardwareBody;
import cc.blynk.server.model.Pin;

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
                if (pins[i].isSame(body)) {
                    pins[i].value = (body.value.length > 1 ? body.value[i] : body.value[0]);
                }
            }
        }
    }

    @Override
    public boolean isSame(HardwareBody body) {
        if (pins != null) {
            for (Pin pin : pins) {
                if (pin.isSame(body)) {
                    return true;
                }
            }
        }
        return false;
    }
}
