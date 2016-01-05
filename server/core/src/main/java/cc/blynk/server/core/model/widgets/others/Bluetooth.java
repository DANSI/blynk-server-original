package cc.blynk.server.core.model.widgets.others;

import cc.blynk.server.core.model.HardwareBody;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Bluetooth extends Widget {

    public String name;

    @Override
    public void updateIfSame(HardwareBody body) {

    }

    @Override
    public boolean isSame(byte pin, PinType type) {
        return false;
    }

    @Override
    public String getValue(byte pin, PinType type) {
        return null;
    }

    @Override
    public String getJsonValue() {
        return null;
    }

    @Override
    public String makeHardwareBody() {
        return null;
    }
}
