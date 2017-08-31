package cc.blynk.server.core.model.widgets.ui.table;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.09.16.
 */
public class Column {

    public final String name;

    @JsonCreator
    public Column(@JsonProperty("name") String name) {
        this.name = name;
    }
}
