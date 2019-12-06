package cc.blynk.server.core.model.widgets.others.rtc;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.widgets.NoPinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.utils.DateTimeUtils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class RTC extends NoPinWidget {

    @JsonSerialize(using = ZoneIdToString.class)
    @JsonDeserialize(using = StringToZoneId.class, as = ZoneId.class)
    public ZoneId tzName;

    @Override
    //supports only virtual pins
    public PinMode getModeType() {
        return null;
    }

    @Override
    public int getPrice() {
        return 100;
    }

    @Override
    public void updateValue(Widget oldWidget) {
        if (oldWidget instanceof RTC) {
            this.tzName = ((RTC) oldWidget).tzName;
        }
    }

    public long getTime() {
        ZoneId zone;
        if (tzName != null) {
            zone = tzName;
        } else {
            zone = DateTimeUtils.UTC;
        }

        LocalDateTime ldt = LocalDateTime.now(zone);
        return ldt.toEpochSecond(ZoneOffset.UTC);
    }

    @Override
    public String getJsonValue() {
        return "[" + getTime() + "]";
    }
}
