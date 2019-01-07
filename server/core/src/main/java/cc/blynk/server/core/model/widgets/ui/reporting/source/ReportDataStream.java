package cc.blynk.server.core.model.widgets.ui.reporting.source;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.07.15.
 */
public class ReportDataStream {

    public final short pin;

    public final PinType pinType;

    public final String label;

    public final boolean isSelected;

    @JsonCreator
    public ReportDataStream(@JsonProperty("pin") short pin,
                            @JsonProperty("pinType") PinType pinType,
                            @JsonProperty("label") String label,
                            @JsonProperty("isSelected") boolean isSelected) {
        this.pin = pin;
        this.pinType = pinType;
        this.label = label;
        this.isSelected = isSelected;
    }

    public boolean isSame(short pin, PinType pinType) {
        return this.pin == pin && this.pinType == pinType;
    }

    public boolean isValid() {
        return this.isSelected && DataStream.isValid(pin, pinType);
    }

    public String formatForFileName() {
        if (label == null || label.isEmpty()) {
            return pinType.pinTypeString + pin;
        }
        return StringUtils.truncate(label.replaceAll("[^a-zA-Z0-9]", ""), 16);
    }

    public String formatAndEscapePin() {
        if (label == null || label.isEmpty()) {
            return pinType.pinTypeString + pin;
        }
        String truncated = StringUtils.truncate(label, 16);
        return StringUtils.escapeCSV(truncated);
    }
}
