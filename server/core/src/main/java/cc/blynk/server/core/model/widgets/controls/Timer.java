package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Timer extends OnePinWidget {

    public Long startTime;

    public String startValue;

    public Long stopTime;

    public String stopValue;

    @Override
    public String getModeType() {
        return "out";
    }
}
