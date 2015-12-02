package cc.blynk.server.model.widgets;

import cc.blynk.server.model.HardwareBody;
import cc.blynk.server.model.enums.PinType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.12.15.
 */
//todo all this should be replaced with 1 Pin field.
public abstract class OnePinWidget extends Widget {

    public PinType pinType;

    public Byte pin;

    public boolean pwmMode;

    public boolean rangeMappingOn;

    public int min;

    public int max;

    public String value;

    public void updateIfSame(HardwareBody body) {
        if (isSame(body.pin, body.type)) {
            value = body.value[0];
        }
    }

    public boolean isSame(byte pin, PinType type) {
        return this.pin != null && this.pin == pin && ((this.pwmMode && type == PinType.ANALOG) || (type == this.pinType));
    }

    public String getValue(byte pin, PinType type) {
        return value;
    }

}
