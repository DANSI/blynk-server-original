package cc.blynk.server.core.model.widgets.ui;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.03.16.
 */
public class Tab {

    public final String label;

    @JsonCreator
    public Tab(@JsonProperty("label") String label) {
        this.label = label;
    }
}
