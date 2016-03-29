package cc.blynk.server.core.model.widgets.others;

import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class RTC extends OnePinWidget {

    public String timezone;

    @Override
    public String getModeType() {
        return null;
    }

    @Override
    public String makeHardwareBody() {
        return null;
    }

    @Override
    public int getPrice() {
        return 100;
    }
}
