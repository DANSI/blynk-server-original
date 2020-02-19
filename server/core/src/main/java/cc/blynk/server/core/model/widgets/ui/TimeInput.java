package cc.blynk.server.core.model.widgets.ui;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.others.rtc.StringToZoneId;
import cc.blynk.server.core.model.widgets.others.rtc.ZoneIdToString;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.ZoneId;

import static cc.blynk.utils.StringUtils.BODY_SEPARATOR_STRING;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27.08.16.
 */
public class TimeInput extends OnePinWidget {

    private static final int NEVER = -1;
    private static final int SUNSET = -2;
    private static final int SUNRISE = -3;

    public String format;

    //from 1 to 7, starts from MONDAY (1)
    public volatile int[] days;

    public volatile int startAt = -1;

    public volatile int stopAt = -1;

    @JsonSerialize(using = ZoneIdToString.class)
    @JsonDeserialize(using = StringToZoneId.class, as = ZoneId.class)
    public volatile ZoneId tzName;

    public boolean isStartStopAllowed;

    public boolean isDayOfWeekAllowed;

    public boolean isSunsetSunriseAllowed;

    public boolean isTimezoneAllowed;

    @Override
    public boolean updateIfSame(int deviceId, short pin, PinType type, String value) {
        if (super.updateIfSame(deviceId, pin, type, value)) {
            String[] values = value.split(BODY_SEPARATOR_STRING);
            if (values.length > 2) {
                startAt = calcTime(values[0]);
                stopAt = calcTime(values[1]);
                tzName = StringToZoneId.parseZoneId(values[2]);
                if (values.length == 3 || values[3].isEmpty()) {
                    days = null;
                } else {
                    String[] daysString = values[3].split(",");
                    days = new int[daysString.length];
                    for (int i = 0; i < daysString.length; i++) {
                        days[i] = Integer.parseInt(daysString[i]);
                    }
                }
            }
            return true;
        }
        return false;
    }

    private static int calcTime(String value) {
        switch (value) {
            case "ss" :
                return SUNSET;
            case "sr" :
                return SUNRISE;
            case "" :
                return NEVER;
            default :
                return Integer.parseInt(value);
        }
    }

    @Override
    public void updateValue(Widget oldWidget) {
        if (oldWidget instanceof TimeInput) {
            TimeInput oldTimeInput = (TimeInput) oldWidget;
            if (oldTimeInput.value != null) {
                this.updateIfSame(oldTimeInput.deviceId, oldTimeInput.pin, oldTimeInput.pinType, oldTimeInput.value);
            }
        }
    }

    @Override
    public void erase() {
        super.erase();
        this.tzName = null;
    }

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public int getPrice() {
        return 200;
    }
}
