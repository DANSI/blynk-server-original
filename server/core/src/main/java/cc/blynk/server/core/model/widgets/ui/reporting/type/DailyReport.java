package cc.blynk.server.core.model.widgets.ui.reporting.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
public class DailyReport extends BaseReportType {

    public final long atTime;

    public final ReportDurationType durationType;

    public final long startTs;

    public final long endTs;

    @JsonCreator
    public DailyReport(@JsonProperty("atTime") long atTime,
                       @JsonProperty("durationType") ReportDurationType durationType,
                       @JsonProperty("startTs") long startTs,
                       @JsonProperty("endTs")long endTs) {
        this.atTime = atTime;
        this.durationType = durationType;
        this.startTs = startTs;
        this.endTs = endTs;
    }

    @Override
    public boolean isValid() {
        return startTs <= endTs;
    }

    ZonedDateTime buildZonedStartAt(ZonedDateTime zonedNow, ZoneId zoneId) {
        ZonedDateTime zonedStartAt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(atTime), zoneId);
        zonedStartAt = zonedNow
                .withHour(zonedStartAt.getHour())
                .withMinute(zonedStartAt.getMinute())
                .withSecond(zonedStartAt.getSecond());

        return adjustToStartDate(zonedStartAt, zonedNow, zoneId);
    }

    private ZonedDateTime adjustToStartDate(ZonedDateTime zonedStartAt, ZonedDateTime zonedNow, ZoneId zoneId) {
        if (durationType == ReportDurationType.CUSTOM) {
            ZonedDateTime zonedStartDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTs), zoneId)
                    .with(LocalTime.MIN);
            if (zonedStartDate.isAfter(zonedNow)) {
                zonedStartAt = zonedStartAt
                        .withDayOfMonth(zonedStartDate.getDayOfMonth())
                        .withMonth(zonedStartDate.getMonthValue())
                        .withYear(zonedStartDate.getYear());
            }
        }
        return zonedStartAt;
    }

    public boolean isExpired(ZonedDateTime zonedNow, ZoneId zoneId) {
        if (durationType == ReportDurationType.CUSTOM) {
            ZonedDateTime zonedEndDate =
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTs), zoneId).with(LocalTime.MAX);
            return zonedEndDate.isBefore(zonedNow);
        }
        return false;
    }

    @Override
    public ZonedDateTime getNextTriggerTime(ZonedDateTime zonedNow, ZoneId zoneId) {
        ZonedDateTime zonedStartAt = buildZonedStartAt(zonedNow, zoneId);
        return zonedStartAt.isAfter(zonedNow) ? zonedStartAt : zonedStartAt.plusDays(1);
    }
}
