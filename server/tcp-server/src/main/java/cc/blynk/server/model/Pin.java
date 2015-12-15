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

    public String label;

    public boolean isSame(byte pin, PinType type) {
        return this.pin != null && this.pin == pin && ((this.pwmMode && type == PinType.ANALOG) || (type == this.pinType));
    }

}
