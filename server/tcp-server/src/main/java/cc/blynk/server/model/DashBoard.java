package cc.blynk.server.model;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.server.exceptions.IllegalCommandBodyException;
import cc.blynk.server.model.widgets.Widget;
import cc.blynk.server.model.widgets.others.Timer;
import cc.blynk.server.model.widgets.outputs.FrequencyWidget;
import cc.blynk.server.utils.JsonParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 13:04
 */
public class DashBoard {

    public int id;

    public String name;

    public Long timestamp;

    public Widget[] widgets = {};

    public String boardType;

    public boolean keepScreenOn;

    public boolean isSharedPublic;

    public boolean isActive;

    /**
     * Specific property used for improving user experience on mobile application.
     * In case user activated dashboard before hardware connected to server, user have to
     * deactivate and activate dashboard again in order to setup PIN MODES (OUT, IN).
     * With this property problem resolved by server side. Command for setting Pin Modes
     * is remembered and when hardware goes online - server sends Pin Modes command to hardware
     * without requiring user to activate/deactivate dashboard again.
     */
    public transient StringMessage pinModeMessage;

    public List<Timer> getTimerWidgets() {
        if (widgets.length == 0) {
            return Collections.emptyList();
        }

        List<Timer> timerWidgets = new ArrayList<>();
        for (Widget widget : widgets) {
            if (widget instanceof Timer) {
                Timer timer = (Timer) widget;
                if ((timer.startTime != null && timer.startValue != null && !timer.startValue.equals("")) ||
                    (timer.stopTime != null && timer.stopValue != null && !timer.stopValue.equals(""))) {
                    timerWidgets.add(timer);
                }
            }
        }

        return timerWidgets;
    }

    public void update(HardwareBody hardwareBody) {
        for (Widget widget : widgets) {
            widget.updateIfSame(hardwareBody);
        }
    }

    public FrequencyWidget findReadingWidget(HardwareBody hardwareBody, int msgId) {
        for (Widget widget : widgets) {
            if (widget instanceof FrequencyWidget && widget.isSame(hardwareBody)) {
                return (FrequencyWidget) widget;
            }
        }
        throw new IllegalCommandBodyException("No frequency widget for read command.", msgId);
    }

    public  <T> T getWidgetByType(Class<T> clazz) {
        if (!isActive || widgets.length == 0) {
            return null;
        }

        for (Widget widget : widgets) {
            if (clazz.isInstance(widget)) {
                return clazz.cast(widget);
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DashBoard dashBoard = (DashBoard) o;

        if (id != dashBoard.id) return false;
        if (name != null ? !name.equals(dashBoard.name) : dashBoard.name != null) return false;
        if (!Arrays.equals(widgets, dashBoard.widgets)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (id ^ (id >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (widgets != null ? Arrays.hashCode(widgets) : 0);
        return result;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }
}
