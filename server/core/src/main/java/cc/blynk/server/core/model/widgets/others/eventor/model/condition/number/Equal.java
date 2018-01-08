package cc.blynk.server.core.model.widgets.others.eventor.model.condition.number;

import cc.blynk.server.core.model.widgets.others.eventor.model.condition.BaseCondition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Equal extends BaseCondition {

    private final double value;

    @JsonCreator
    public Equal(@JsonProperty("value") double value) {
        this.value = value;
    }

    @Override
    public boolean matches(String inString, double in) {
        return Double.compare(in, value) == 0;
    }

}
