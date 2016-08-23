package cc.blynk.server.core.model.widgets.others.eventor.model.action;

import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.auth.Session;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class SetPin extends BaseAction {

    public Pin pin;

    public String value;

    public SetPin() {
    }

    public SetPin(Pin pin, String value) {
        this.pin = pin;
        this.value = value;
    }

    public void execute(Session session, int dashId) {
        if (pin != null && pin.pinType != null && pin.pin > -1 && value != null) {
            String body = Pin.makeHardwareBody(pin.pwmMode, pin.pinType, pin.pin, value);
            session.sendMessageToHardware(dashId, HARDWARE, 888, body);
        }
    }

}
