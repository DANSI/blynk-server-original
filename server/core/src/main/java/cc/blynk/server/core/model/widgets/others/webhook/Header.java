package cc.blynk.server.core.model.widgets.others.webhook;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.09.16.
 */
public class Header {

    public final String name;

    public final String value;

    @JsonCreator
    public Header(@JsonProperty("name") String name,
                  @JsonProperty("value") String value) {
        this.name = name;
        this.value = value;
    }

    public boolean isValid() {
        return name != null && value != null;
    }
}
