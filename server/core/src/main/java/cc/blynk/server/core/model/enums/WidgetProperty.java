package cc.blynk.server.core.model.enums;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.11.17.
 */
public enum WidgetProperty {

    LABEL("label"),
    COLOR("color"),
    ON_BACK_COLOR("onBackColor"),
    OFF_BACK_COLOR("offBackColor"),
    ON_COLOR("onColor"),
    OFF_COLOR("offColor"),
    ON_LABEL("onLabel"),
    OFF_LABEL("offLabel"),
    LABELS("labels"),
    MIN("min"),
    MAX("max"),
    IS_ON_PLAY("isOnPlay"),
    URL("url"),
    URLS("urls"),
    STEP("step"),
    VALUE_FORMATTING("valueFormatting"),
    SUFFIX("suffix"),
    FRACTION("maximumFractionDigits"),
    OPACITY("opacity"),
    SCALE("scale"),
    ROTATION("rotation");

    public final String label;

    private static final WidgetProperty[] values = values();

    WidgetProperty(String label) {
        this.label = label;
    }

    public static WidgetProperty getProperty(String value) {
        switch (value) {
            case "label" :
                return LABEL;
            case "color" :
                return COLOR;
            case "onLabel" :
                return ON_LABEL;
            case "onColor" :
                return ON_COLOR;
            case "onBackColor" :
                return ON_BACK_COLOR;
            case "offLabel" :
                return OFF_LABEL;
            case "offColor" :
                return OFF_COLOR;
            case "offBackColor" :
                return OFF_BACK_COLOR;
            case "labels" :
                return LABELS;
            case "min" :
                return MIN;
            case "max" :
                return MAX;
            case "isOnPlay" :
                return IS_ON_PLAY;
            case "url" :
                return URL;
            case "urls" :
                return URLS;
            case "step" :
                return STEP;
            case "valueFormatting" :
                return VALUE_FORMATTING;
            case "suffix" :
                return SUFFIX;
            case "maximumFractionDigits" :
                return FRACTION;
            case "opacity" :
                return OPACITY;
            case "scale" :
                return SCALE;
            case "rotation" :
                return ROTATION;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return label;
    }

    public static WidgetProperty[] getValues() {
        return values;
    }
}
