package cc.blynk.server.core.model.widgets.others.eventor.model.action;

import cc.blynk.server.core.model.Pin;

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

}
