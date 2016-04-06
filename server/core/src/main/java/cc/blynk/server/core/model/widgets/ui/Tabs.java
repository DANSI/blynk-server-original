package cc.blynk.server.core.model.widgets.ui;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.02.16.
 */
public class Tabs extends Widget {

    public Tab[] tabs;

    public Tabs() {
        this.tabId = -1;
    }

    @Override
    public void updateIfSame(byte pin, PinType type, String[] values) {

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
    public String makeHardwareBody() {
        return null;
    }

    @Override
    public String getModeType() {
        return null;
    }

    @Override
    public int getPrice() {
        return 0;
    }

}
