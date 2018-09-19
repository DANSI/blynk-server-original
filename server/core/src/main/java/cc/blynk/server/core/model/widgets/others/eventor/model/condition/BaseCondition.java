package cc.blynk.server.core.model.widgets.others.eventor.model.condition;

import cc.blynk.server.core.model.widgets.others.eventor.model.condition.number.Between;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.number.Equal;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.number.GreaterThan;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.number.GreaterThanOrEqual;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.number.LessThan;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.number.LessThanOrEqual;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.number.NotBetween;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.number.NotEqual;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.string.StringEqual;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.string.StringNotEqual;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GreaterThan.class, name = "GT"),
        @JsonSubTypes.Type(value = GreaterThanOrEqual.class, name = "GTE"),
        @JsonSubTypes.Type(value = LessThan.class, name = "LT"),
        @JsonSubTypes.Type(value = LessThanOrEqual.class, name = "LTE"),
        @JsonSubTypes.Type(value = Equal.class, name = "EQ"),
        @JsonSubTypes.Type(value = NotEqual.class, name = "NEQ"),
        @JsonSubTypes.Type(value = Between.class, name = "BETWEEN"),
        @JsonSubTypes.Type(value = NotBetween.class, name = "NOT_BETWEEN"),
        @JsonSubTypes.Type(value = ValueChanged.class, name = "CHANGED"),

        @JsonSubTypes.Type(value = StringEqual.class, name = "STR_EQUAL"),
        @JsonSubTypes.Type(value = StringNotEqual.class, name = "STR_NOT_EQUAL")
})
public abstract class BaseCondition {

    public abstract boolean matches(String inString, double in);

}
