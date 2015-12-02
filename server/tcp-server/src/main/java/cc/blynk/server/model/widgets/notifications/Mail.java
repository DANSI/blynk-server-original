package cc.blynk.server.model.widgets.notifications;

import cc.blynk.server.model.HardwareBody;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.model.widgets.Widget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Mail extends Widget {

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
}
