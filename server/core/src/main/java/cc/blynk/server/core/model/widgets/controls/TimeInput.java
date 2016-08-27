package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27.08.16.
 */
public class TimeInput extends OnePinWidget {

    public String format;

    public boolean isStartStopAllowed;

    public boolean isDayOfWeekAllowed;

    public boolean isSunsetSunriseAllowed;

    public boolean isTimezoneAllowed;

    @Override
    public String getModeType() {
        return "out";
    }

    @Override
    public int getPrice() {
        return 0;
    }
}
