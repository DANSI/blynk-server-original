package cc.blynk.server.core.model.widgets.others.eventor.model.action;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.WidgetProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.11.18.
 */
public class SetPropertyPinAction extends BaseAction {

    @JsonProperty("pin") //todo "pin" for back compatibility
    public final DataStream dataStream;

    public final WidgetProperty property;

    public final String value;

    @JsonCreator
    public SetPropertyPinAction(@JsonProperty("pin") DataStream dataStream,
                                @JsonProperty("property") WidgetProperty property,
                                @JsonProperty("value") String value) {
        this.dataStream = dataStream;
        this.property = property;
        this.value = value;
    }

    public String makeHardwareBody() {
        return DataStream.makePropertyHardwareBody(dataStream.pin, property, value);
    }

    @Override
    public boolean isValid() {
        return dataStream != null
                && dataStream.pinType != null
                && dataStream.pin > -1
                && value != null && !value.isEmpty();
    }
}
