package cc.blynk.server.model;

import cc.blynk.server.model.enums.PinType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.07.15.
 */
public class Pin {

    public Byte pin;

    public boolean pwmMode;

    public boolean rangeMappingOn;

    public PinType pinType;

    public String value;

    public Integer min;

    public Integer max;

    public boolean isSame(HardwareBody body) {
        return pin != null && pin == body.pin && ((pwmMode && body.type == PinType.ANALOG) || (body.type == pinType));
    }

}
