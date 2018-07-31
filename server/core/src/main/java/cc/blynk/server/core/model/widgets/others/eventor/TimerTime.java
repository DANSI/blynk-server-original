package cc.blynk.server.core.model.widgets.others.eventor;

import cc.blynk.server.core.model.widgets.others.rtc.StringToZoneId;
import cc.blynk.server.core.model.widgets.others.rtc.ZoneIdToString;
import cc.blynk.server.internal.EmptyArraysUtil;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 30.01.17.
 */
public class TimerTime {

    private static final int[] ALL_DAYS = new int[] {1, 2, 3, 4, 5, 6, 7};

    public final int id;

    public final int[] days;

    public final int time;

    @JsonSerialize(using = ZoneIdToString.class)
    @JsonDeserialize(using = StringToZoneId.class, as = ZoneId.class)
    public final ZoneId tzName;

    @JsonCreator
    public TimerTime(@JsonProperty("id") int id,
                     @JsonProperty("days") int[] days,
                     @JsonProperty("time") int time,
                     @JsonProperty("tzName") ZoneId tzName) {
        this.id = id;
        this.days = days == null ? EmptyArraysUtil.EMPTY_INTS : days;
        this.time = time;
        this.tzName = tzName;
    }

    public TimerTime(int time) {
        this(0, ALL_DAYS, time, DateTimeUtils.UTC);
    }

    public boolean isTickTime(ZonedDateTime currentDateTime) {
        LocalDate userDate = currentDateTime.withZoneSameInstant(tzName).toLocalDate();
        int dayOfWeek = userDate.getDayOfWeek().getValue();
        return ArrayUtil.contains(days, dayOfWeek);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimerTime)) {
            return false;
        }

        TimerTime timerTime = (TimerTime) o;

        if (id != timerTime.id) {
            return false;
        }
        if (time != timerTime.time) {
            return false;
        }
        if (!Arrays.equals(days, timerTime.days)) {
            return false;
        }
        return !(tzName != null ? !tzName.equals(timerTime.tzName) : timerTime.tzName != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (days != null ? Arrays.hashCode(days) : 0);
        result = 31 * result + time;
        result = 31 * result + (tzName != null ? tzName.hashCode() : 0);
        return result;
    }
}
