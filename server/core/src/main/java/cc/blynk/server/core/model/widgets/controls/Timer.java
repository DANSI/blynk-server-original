package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Timer extends OnePinWidget {

    public long startTime = -1;

    public String startValue;

    public long stopTime = -1;

    public String stopValue;

    public boolean invertedOn = false;

    @Override
    public String getModeType() {
        return "out";
    }

    @Override
    public String makeHardwareBody() {
        return null;
    }

    @Override
    public int getPrice() {
        return 200;
    }
}
