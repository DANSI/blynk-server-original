package cc.blynk.server.core.model.widgets.ui.reporting.source;

import cc.blynk.server.core.model.enums.PinType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.07.15.
 */
public class ReportDataStream {

    public final byte pin;

    public final PinType pinType;

    public final String label;

    @JsonCreator
    public ReportDataStream(@JsonProperty("pin") byte pin,
                            @JsonProperty("pinType") PinType pinType,
                            @JsonProperty("label") String label) {
        this.pin = pin;
        this.pinType = pinType;
        this.label = label;
    }
}
