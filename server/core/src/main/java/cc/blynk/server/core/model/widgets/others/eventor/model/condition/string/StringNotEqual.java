package cc.blynk.server.core.model.widgets.others.eventor.model.condition.string;

import cc.blynk.server.core.model.widgets.others.eventor.model.condition.BaseCondition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class StringNotEqual extends BaseCondition {

    private final String value;

    @JsonCreator
    public StringNotEqual(@JsonProperty("value") String value) {
        this.value = value;
    }

    @Override
    public boolean matches(String inString, double in) {
        return !inString.equals(value);
    }

}
