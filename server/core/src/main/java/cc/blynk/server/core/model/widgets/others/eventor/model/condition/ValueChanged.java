package cc.blynk.server.core.model.widgets.others.eventor.model.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class ValueChanged extends BaseCondition {

    private volatile String value;

    @JsonCreator
    public ValueChanged(@JsonProperty("value") String value) {
        this.value = value;
    }

    @Override
    public boolean matches(String inString, double in) {
        if (inString.equals(value)) {
            return false;
        }
        value = inString;
        return true;
    }

}
