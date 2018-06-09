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

    private final long atTime;

    private final ReportDurationType durationType;

    private final long startTs;

    private final long endTs;

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

    @Override
    public long getDuration() {
        return 1;
    }

    static ZonedDateTime getZonedFromTs(long ts, ZoneId zoneId) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), zoneId);
    }

    @Override
    public String getDurationLabel() {
        return "Daily";
    }

    @Override
    public void buildDynamicSection(StringBuilder sb, ZoneId zoneId) {
        sb.append("Period: ").append(getDurationLabel());
        addReportSpecificAtTime(sb, zoneId);

        if (durationType == ReportDurationType.CUSTOM) {
            sb.append("<br>");

            ZonedDateTime date;

            date = getZonedFromTs(startTs, zoneId);
            sb.append("Start date: ").append(date.toLocalDate()).append("<br>");

            date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTs), zoneId);
            sb.append("End date: ").append(date.toLocalDate()).append("<br>");
        }
    }

    public void addReportSpecificAtTime(StringBuilder sb, ZoneId zoneId) {
        ZonedDateTime zonedAt = getZonedFromTs(atTime, zoneId);
        LocalTime localTime = zonedAt.toLocalTime();
        sb.append(", ").append("at ").append(LocalTime.of(localTime.getHour(), localTime.getMinute()));
    }

    ZonedDateTime buildZonedStartAt(ZonedDateTime zonedNow, ZoneId zoneId) {
        ZonedDateTime zonedStartAt = getZonedFromTs(atTime, zoneId);
        zonedStartAt = zonedNow
                .withHour(zonedStartAt.getHour())
                .withMinute(zonedStartAt.getMinute())
                .withSecond(zonedStartAt.getSecond());

        return adjustToStartDate(zonedStartAt, zonedNow, zoneId);
    }

    private ZonedDateTime adjustToStartDate(ZonedDateTime zonedStartAt, ZonedDateTime zonedNow, ZoneId zoneId) {
        if (durationType == ReportDurationType.CUSTOM) {
            ZonedDateTime zonedStartDate = getZonedFromTs(startTs, zoneId).with(LocalTime.MIN);
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
            ZonedDateTime zonedEndDate = getZonedFromTs(endTs, zoneId).with(LocalTime.MAX);
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
