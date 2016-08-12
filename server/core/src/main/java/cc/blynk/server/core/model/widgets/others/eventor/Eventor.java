package cc.blynk.server.core.model.widgets.others.eventor;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.NoPinWidget;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.SetPin;
import io.netty.channel.Channel;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Eventor extends NoPinWidget {

    public Rule[] rules;

    public Eventor() {
    }

    public Eventor(Rule[] rules) {
        this.rules = rules;
    }

    @Override
    public boolean updateIfSame(byte pin, PinType type, String value) {
        return false;
    }

    @Override
    public boolean isSame(byte pin, PinType type) {
        return false;
    }

    @Override
    public String getJsonValue() {
        return null;
    }

    @Override
    public void sendSyncOnActivate(Channel appChannel, int dashId) {

    }

    @Override
    public String getModeType() {
        return "out";
    }

    @Override
    public String getValue(byte pin, PinType type) {
        return null;
    }

    @Override
    public boolean hasValue(String searchValue) {
        return false;
    }

    @Override
    public void append(StringBuilder sb) {
        if (rules != null) {
            for (Rule rule : rules) {
                if (rule.actions != null) {
                    for (BaseAction action : rule.actions) {
                        if (action instanceof SetPin) {
                            SetPin setPinAction = (SetPin) action;
                            if (setPinAction.pin != null) {
                                append(sb, setPinAction.pin.pin, setPinAction.pin.pinType, getModeType());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getPrice() {
        return 500;
    }
}
