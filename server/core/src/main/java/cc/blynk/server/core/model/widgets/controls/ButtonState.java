package cc.blynk.server.core.model.widgets.controls;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.18.
 */
public class ButtonState {

    public volatile String text;

    public volatile int textColor;

    public volatile int backgroundColor;

    public String iconName;

    @JsonCreator
    public ButtonState(@JsonProperty("text") String text,
                       @JsonProperty("textColor") int textColor,
                       @JsonProperty("backgroundColor") int backgroundColor,
                       @JsonProperty("iconName") String iconName) {
        this.text = text;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
        this.iconName = iconName;
    }
}
