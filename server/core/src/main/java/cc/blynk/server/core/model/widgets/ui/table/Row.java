package cc.blynk.server.core.model.widgets.ui.table;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.09.16.
 */
public class Row {

    public final int id;

    public volatile String name;

    public volatile String value;

    public volatile boolean isSelected;

    @JsonCreator
    public Row(@JsonProperty("id") int id,
               @JsonProperty("name") String name,
               @JsonProperty("value") String value,
               @JsonProperty("isSelected") boolean isSelected) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.isSelected = isSelected;
    }

    public void update(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return "add" + BODY_SEPARATOR
                + id + BODY_SEPARATOR
                + name + BODY_SEPARATOR
                + value + BODY_SEPARATOR
                + isSelected;
    }
}
