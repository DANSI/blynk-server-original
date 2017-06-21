package cc.blynk.server.core.model.widgets.others.eventor.model.action;

import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class SetPinAction extends BaseAction {

    public final Pin pin;

    public final String value;

    private final SetPinActionType setPinType;

    @JsonCreator
    public SetPinAction(@JsonProperty("pin") Pin pin,
                        @JsonProperty("value") String value,
                        @JsonProperty("setPinType") SetPinActionType setPinType) {
        this.pin = pin;
        this.value = value;
        this.setPinType = setPinType;
    }

    public SetPinAction(byte pin, PinType pinType, String value) {
        this.pin = new Pin(pin, pinType);
        //this is dirty hack for back compatibility.
        //this is mistakes of our youth. sorry for that :).
        //todo remove some day in future.
        if (value.contains(StringUtils.BODY_SEPARATOR_STRING)) {
            String[] split = StringUtils.split3(value);
            this.value = split[2];
        } else {
            this.value = value;
        }
        this.setPinType = SetPinActionType.CUSTOM;
    }

    public String makeHardwareBody() {
        return Pin.makeHardwareBody(pin.pwmMode, pin.pinType, pin.pin, value);
    }

    @Override
    public boolean isValid() {
        return pin != null && pin.pinType != null && pin.pin > -1 && value != null;
    }
}
