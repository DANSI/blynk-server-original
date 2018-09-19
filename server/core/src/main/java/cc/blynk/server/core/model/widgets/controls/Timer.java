package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Timer extends OnePinWidget {

    public int startTime = -1;

    public String startValue;

    public int stopTime = -1;

    public String stopValue;

    public FontSize fontSize;

    public boolean isValidStart() {
        return isValidTime(startTime) && isValidValue(startValue);
    }

    public boolean isValidStop() {
        return isValidTime(stopTime) && isValidValue(stopValue);
    }

    public static boolean isValidTime(int time) {
        return time > -1 && time < 86400;
    }

    private static boolean isValidValue(String value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public int getPrice() {
        return 200;
    }

    @Override
    public void erase() {
        super.erase();
        this.startValue = null;
        this.stopValue = null;
        this.startTime = -1;
        this.stopTime = -1;
    }

    @Override
    public void updateValue(Widget oldWidget) {
        if (oldWidget instanceof Timer) {
            Timer oldTimer = (Timer) oldWidget;
            if (isSame(oldTimer.deviceId, oldTimer.pin, oldTimer.pinType)) {
                if (oldTimer.value != null) {
                    this.value = oldTimer.value;
                }
                if (oldTimer.startTime != -1) {
                    this.startTime = oldTimer.startTime;
                }
                if (oldTimer.stopTime != -1) {
                    this.stopTime = oldTimer.stopTime;
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Timer)) {
            return false;
        }

        Timer timer = (Timer) o;

        return id == timer.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
