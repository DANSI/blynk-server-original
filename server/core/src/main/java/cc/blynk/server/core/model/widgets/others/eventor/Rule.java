package cc.blynk.server.core.model.widgets.others.eventor;

import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.BaseCondition;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Rule {

    public Pin triggerPin;

    public BaseCondition condition;

    public BaseAction[] actions;

    public boolean isActive;

    public Rule() {
    }

    public Rule(Pin triggerPin, BaseCondition condition, BaseAction[] actions) {
        this.triggerPin = triggerPin;
        this.condition = condition;
        this.actions = actions;
    }

    public boolean isValid(byte pin, PinType pinType, double value) {
        return isActive && triggerPin != null && condition != null &&
               actions != null && triggerPin.isSame(pin, pinType) &&
               condition.isValid(value);
    }

}
