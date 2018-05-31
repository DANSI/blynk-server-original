package cc.blynk.server.core.model.widgets.ui.reporting.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
public class WeeklyReport extends DailyReport {

    //starts from MONDAY (1)
    public final int dayOfTheWeek;

    @JsonCreator
    public WeeklyReport(@JsonProperty("atTime") long atTime,
                        @JsonProperty("durationType") ReportDurationType durationType,
                        @JsonProperty("startTs") long startTs,
                        @JsonProperty("endTs") long endTs,
                        @JsonProperty("dayOfTheWeek") int dayOfTheWeek) {
        super(atTime, durationType, startTs, endTs);
        this.dayOfTheWeek = dayOfTheWeek;
    }

    @Override
    public ZonedDateTime getNextTriggerTime(ZonedDateTime zonedNow, ZoneId zoneId) {
        ZonedDateTime zonedStartAt = buildZonedStartAt(zonedNow, zoneId);

        DayOfWeek dayOfWeek = DayOfWeek.of(dayOfTheWeek);
        zonedStartAt = zonedStartAt.with(TemporalAdjusters.nextOrSame(dayOfWeek));
        return zonedStartAt.isAfter(zonedNow) ?
                zonedStartAt : zonedStartAt.with(TemporalAdjusters.next(dayOfWeek));
    }
}
