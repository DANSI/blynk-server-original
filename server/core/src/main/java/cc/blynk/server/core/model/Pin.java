package cc.blynk.server.core.model;

import cc.blynk.server.core.model.enums.PinType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.07.15.
 */
public class Pin {

    public static final int NO_PIN = -1;

    public final byte pin;

    public final boolean pwmMode;

    public final boolean rangeMappingOn;

    public final PinType pinType;

    public volatile String value;

    public final int min;

    public final int max;

    public final String label;

    @JsonCreator
    public Pin(@JsonProperty("pin") byte pin,
               @JsonProperty("pwmMode") boolean pwmMode,
               @JsonProperty("rangeMappingOn") boolean rangeMappingOn,
               @JsonProperty("pinType") PinType pinType,
               @JsonProperty("value") String value,
               @JsonProperty("min") int min,
               @JsonProperty("max") int max,
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

    public Pin(byte pin, PinType pinType) {
        this(pin, false, false, pinType, null, 0, 255, null);
    }

    public static String makeReadingHardwareBody(char pinType, byte pin) {
        return "" + pinType + 'r' + BODY_SEPARATOR + pin;
    }

    public static String makeHardwareBody(char pinType, String pin, String value) {
        return "" + pinType + 'w' + BODY_SEPARATOR + pin + BODY_SEPARATOR + value;
    }

    public static String makeHardwareBody(PinType pinType, byte pin, String value) {
        return makeHardwareBody(pinType.pintTypeChar, pin, value);
    }

    public static String makeHardwareBody(char pinTypeChar, byte pin, String value) {
        return "" + pinTypeChar + 'w' + BODY_SEPARATOR + pin + BODY_SEPARATOR + value;
    }

    public static String makeHardwareBody(boolean pwmMode, PinType pinType, byte pin, String value) {
        return pwmMode ? makeHardwareBody(PinType.ANALOG, pin, value) : makeHardwareBody(pinType, pin, value);
    }

    public boolean isSame(byte pin, PinType type) {
        return this.pin == pin && ((this.pwmMode && type == PinType.ANALOG) || (type == this.pinType));
    }

    public String makeHardwareBody() {
        return pwmMode ? makeHardwareBody(PinType.ANALOG, pin, value) : makeHardwareBody(pinType, pin, value);
    }

    public boolean isNotValid() {
        return pin == NO_PIN || pinType == null;
    }

    public boolean notEmpty() {
        return value != null && !isNotValid();
    }
}
