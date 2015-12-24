package cc.blynk.server.handlers.http.helpers;

import cc.blynk.server.model.enums.PinType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.12.15.
 */
public class SimplePin {

    public final PinType pinType;
    public final byte pin;
    public final String value;

    public SimplePin(String pin) {
        this.pinType = PinType.getPingType(pin.charAt(0));
        this.pin = Byte.parseByte(pin.substring(1));
        this.value = null;
    }

    public SimplePin(String pin, String value) {
        this.pinType = PinType.getPingType(pin.charAt(0));
        this.pin = Byte.parseByte(pin.substring(1));
        this.value = value;
    }

}
