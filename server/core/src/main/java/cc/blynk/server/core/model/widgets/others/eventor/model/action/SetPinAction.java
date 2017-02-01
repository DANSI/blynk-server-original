package cc.blynk.server.core.model.widgets.others.eventor.model.action;

import cc.blynk.server.core.model.Pin;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class SetPinAction extends BaseAction {

    public Pin pin;

    public String value;

    public SetPinAction() {
    }

    public SetPinAction(Pin pin, String value) {
        this.pin = pin;
        this.value = value;
    }

    public String makeHardwareBody() {
        return Pin.makeHardwareBody(pin.pwmMode, pin.pinType, pin.pin, value);
    }

    @Override
    public boolean isValid() {
        return pin != null && pin.pinType != null && pin.pin > -1 && value != null;
    }
}
