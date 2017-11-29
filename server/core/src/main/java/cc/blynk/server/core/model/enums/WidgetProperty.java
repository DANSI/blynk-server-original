package cc.blynk.server.core.model.enums;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.11.17.
 */
public enum WidgetProperty {

    LABEL("label"),
    COLOR("color"),
    ON_LABEL("onLabel"),
    OFF_LABEL("offLabel"),
    LABELS("labels"),
    MIN("min"),
    MAX("max"),
    IS_ON_PLAY("isOnPlay"),
    URL("url");

    public final String label;

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
            case "offLabel" :
                return OFF_LABEL;
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
            default:
                return null;
        }
    }

}
