package cc.blynk.server.core.model;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.WidgetProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.07.15.
 */
public class DataStream {

    public static final int NO_PIN = -1;

    public final short pin;

    public final boolean pwmMode;

    public final boolean rangeMappingOn;

    public final PinType pinType;

    public volatile String value;

    public final float min;

    public final float max;

    public final String label;

    @JsonCreator
    public DataStream(@JsonProperty("pin") short pin,
                      @JsonProperty("pwmMode") boolean pwmMode,
                      @JsonProperty("rangeMappingOn") boolean rangeMappingOn,
                      @JsonProperty("pinType") PinType pinType,
                      @JsonProperty("value") String value,
                      @JsonProperty("min") float min,
                      @JsonProperty("max") float max,
                      @JsonProperty("label") String label) {
        this.pin = pin;
        this.pwmMode = pwmMode;
        this.rangeMappingOn = rangeMappingOn;
        this.pinType = pinType;
        this.value = value;
        this.min = min;
        this.max = max;
        this.label = label;
    }

    public DataStream(DataStream dataStream) {
        this(dataStream.pin, dataStream.pwmMode, dataStream.rangeMappingOn,
                dataStream.pinType, dataStream.value,
                dataStream.min, dataStream.max, dataStream.label);
    }

    public DataStream(short pin, PinType pinType) {
        this(pin, false, false, pinType, null, 0, 255, null);
    }

    public static String makeReadingHardwareBody(char pinType, short pin) {
        return "" + pinType + 'r' + BODY_SEPARATOR + pin;
    }

    public static String makeHardwareBody(char pinType, String pin, String value) {
        return "" + pinType + 'w' + BODY_SEPARATOR + pin + BODY_SEPARATOR + value;
    }

    public static String makeHardwareBody(PinType pinType, short pin, String value) {
        return makeHardwareBody(pinType.pintTypeChar, pin, value);
    }

    public static String makeHardwareBody(char pinTypeChar, short pin, String value) {
        return "" + pinTypeChar + 'w' + BODY_SEPARATOR + pin + BODY_SEPARATOR + value;
    }

    public static String makeHardwareBody(boolean pwmMode, PinType pinType, short pin, String value) {
        return pwmMode ? makeHardwareBody(PinType.ANALOG, pin, value) : makeHardwareBody(pinType, pin, value);
    }

    public static String makePropertyHardwareBody(short pin, WidgetProperty property, String value) {
        return "" + pin + BODY_SEPARATOR + property.label + BODY_SEPARATOR + value;
    }

    public boolean isSame(short pin, PinType type) {
        return this.pin == pin && (type == this.pinType || (this.pwmMode && type == PinType.ANALOG));
    }

    public String makeHardwareBody() {
        return pwmMode ? makeHardwareBody(PinType.ANALOG, pin, value) : makeHardwareBody(pinType, pin, value);
    }

    public static boolean isValid(short pin, PinType pinType) {
        return pin != NO_PIN && pinType != null;
    }

    public boolean isValid() {
        return isValid(pin, pinType);
    }

    public boolean isNotEmpty() {
        return value != null;
    }

    public boolean notEmptyAndIsValid() {
        return isNotEmpty() && isValid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataStream)) {
            return false;
        }

        DataStream that = (DataStream) o;

        if (pin != that.pin) {
            return false;
        }
        if (pwmMode != that.pwmMode) {
            return false;
        }
        if (rangeMappingOn != that.rangeMappingOn) {
            return false;
        }
        if (Float.compare(that.min, min) != 0) {
            return false;
        }
        if (Float.compare(that.max, max) != 0) {
            return false;
        }
        if (pinType != that.pinType) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }
        return label != null ? label.equals(that.label) : that.label == null;
    }

    @Override
    public int hashCode() {
        int result = (int) pin;
        result = 31 * result + (pwmMode ? 1 : 0);
        result = 31 * result + (rangeMappingOn ? 1 : 0);
        result = 31 * result + (pinType != null ? pinType.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (min != +0.0f ? Float.floatToIntBits(min) : 0);
        result = 31 * result + (max != +0.0f ? Float.floatToIntBits(max) : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        return result;
    }
}
