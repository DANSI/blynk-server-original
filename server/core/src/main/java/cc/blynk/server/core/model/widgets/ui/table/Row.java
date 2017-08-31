package cc.blynk.server.core.model.widgets.ui.table;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
}
