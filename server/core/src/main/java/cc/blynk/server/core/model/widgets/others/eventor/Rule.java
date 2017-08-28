package cc.blynk.server.core.model.widgets.others.eventor;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.BaseCondition;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Rule {

    @JsonProperty("triggerPin") //todo "triggerPin" for back compatibility
    public DataStream triggerDataStream;

    public TimerTime triggerTime;

    public BaseCondition condition;

    public BaseAction[] actions;

    public boolean isActive;

    public transient boolean isProcessed;

    public Rule() {
    }

    public Rule(DataStream triggerDataStream, BaseCondition condition, BaseAction[] actions) {
        this.triggerDataStream = triggerDataStream;
        this.condition = condition;
        this.actions = actions;
    }

    private boolean notEmpty() {
        return triggerDataStream != null && condition != null && actions != null;
    }

    public boolean isReady(byte pin, PinType pinType) {
        return isActive && notEmpty() && triggerDataStream.isSame(pin, pinType);
    }

    public boolean isValidTimerRule() {
        return isActive && triggerTime != null && actions != null && actions.length > 0 && actions[0].isValid();
    }

    public boolean isValid(double value) {
        return condition.isValid(value);
    }

}
