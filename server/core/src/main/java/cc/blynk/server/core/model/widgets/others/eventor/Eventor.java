package cc.blynk.server.core.model.widgets.others.eventor;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import io.netty.channel.Channel;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Eventor extends Widget {

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
        return null;
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
    public int getPrice() {
        return 500;
    }
}
