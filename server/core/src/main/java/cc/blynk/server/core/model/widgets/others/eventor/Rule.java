package cc.blynk.server.core.model.widgets.others.eventor;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.BaseCondition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Rule {

    @JsonProperty("triggerPin") //todo "triggerPin" for back compatibility
    public final DataStream triggerDataStream;

    public final TimerTime triggerTime;

    public final BaseCondition condition;

    public final BaseAction[] actions;

    public final boolean isActive;

    public transient boolean isProcessed;

    @JsonCreator
    public Rule(@JsonProperty("triggerPin") DataStream triggerDataStream,
                @JsonProperty("triggerTime") TimerTime triggerTime,
                @JsonProperty("condition") BaseCondition condition,
                @JsonProperty("actions") BaseAction[] actions,
                @JsonProperty("isActive") boolean isActive) {
        this.triggerDataStream = triggerDataStream;
        this.triggerTime = triggerTime;
        this.condition = condition;
        this.actions = actions;
        this.isActive = isActive;
    }

    private boolean notEmpty() {
        return triggerDataStream != null && condition != null && actions != null;
    }

    public boolean isReady(short pin, PinType pinType) {
        return isActive && notEmpty() && triggerDataStream.isSame(pin, pinType);
    }

    public boolean isValidTimerRule() {
        return isActive && triggerTime != null && Timer.isValidTime(triggerTime.time)
         && actions != null && actions.length > 0 && actions[0].isValid();
    }

    public boolean matchesCondition(String inValue, double parsedInValueToDouble) {
        return condition.matches(inValue, parsedInValueToDouble);
    }

}
