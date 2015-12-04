package cc.blynk.server.model.widgets;

import cc.blynk.server.model.HardwareBody;
import cc.blynk.server.model.Pin;
import cc.blynk.server.model.enums.PinType;

import static cc.blynk.common.utils.StringUtils.*;

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

    public static String makeHardwareBody(Pin pin) {
        return makeHardwareBody(pin.pwmMode, pin.pinType, pin.pin, pin.value);
    }

    public static String makeHardwareBody(boolean pwmMode, PinType pinType, byte pin, String value) {
        return pwmMode ? makeHardwareBody(PinType.ANALOG, pin, value) : makeHardwareBody(pinType, pin, value);
    }

    public static String makeHardwareBody(PinType pinType, byte pin, String value) {
        return "" + pinType.pintTypeChar + 'w'
                + BODY_SEPARATOR_STRING + pin
                + BODY_SEPARATOR_STRING + value;
    }

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
