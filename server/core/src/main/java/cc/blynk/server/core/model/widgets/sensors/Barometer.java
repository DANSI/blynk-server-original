package cc.blynk.server.core.model.widgets.sensors;

import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.09.16.
 */
public class Barometer extends OnePinWidget {

    @Override
    public String getModeType() {
        return "out";
    }

    @Override
    public int getPrice() {
        return 300;
    }
}
