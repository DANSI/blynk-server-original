package cc.blynk.server.core.model.widgets.ui.tiles.templates;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.18.
 */
public class State {

    private final int tileColor;

    private final String iconName;

    private final int iconColor;

    private final String text;

    private final int textColor;

    @JsonCreator
    public State(@JsonProperty("tileColor") int tileColor,
                 @JsonProperty("iconName") String iconName,
                 @JsonProperty("iconColor") int iconColor,
                 @JsonProperty("text") String text,
                 @JsonProperty("textColor") int textColor) {
        this.tileColor = tileColor;
        this.iconName = iconName;
        this.iconColor = iconColor;
        this.text = text;
        this.textColor = textColor;
    }
}
