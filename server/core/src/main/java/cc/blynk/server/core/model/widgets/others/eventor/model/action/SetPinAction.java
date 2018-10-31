package cc.blynk.server.core.model.widgets.others.eventor.model.action;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class SetPinAction extends BaseAction {

    @JsonProperty("pin") //todo "pin" for back compatibility
    public final DataStream dataStream;

    public final String value;

    private final SetPinActionType setPinType;

    @JsonCreator
    public SetPinAction(@JsonProperty("pin") DataStream dataStream,
                        @JsonProperty("value") String value,
                        @JsonProperty("setPinType") SetPinActionType setPinType) {
        this.dataStream = dataStream;
        this.value = value;
        this.setPinType = setPinType;
    }

    public SetPinAction(short pin, PinType pinType, String value) {
        this.dataStream = new DataStream(pin, pinType);
        this.value = value;
        this.setPinType = SetPinActionType.CUSTOM;
    }

    public String makeHardwareBody() {
        return DataStream.makeHardwareBody(dataStream.pwmMode, dataStream.pinType, dataStream.pin, value);
    }

    @Override
    public boolean isValid() {
        return dataStream != null && dataStream.pinType != null && dataStream.pin > -1 && value != null;
    }
}
