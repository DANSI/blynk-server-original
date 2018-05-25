package cc.blynk.server.core.model.widgets.ui.reporting.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

import static java.time.temporal.ChronoField.SECOND_OF_DAY;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
public class DailyReportType extends BaseReportType {

    public final long atTime;

    public final ReportDurationType durationType;

    public final long startTs;

    public final long endTs;

    @JsonCreator
    public DailyReportType(@JsonProperty("atTime") long atTime,
                           @JsonProperty("durationType") ReportDurationType durationType,
                           @JsonProperty("startTs") long startTs,
                           @JsonProperty("endTs")long endTs) {
        this.atTime = atTime;
        this.durationType = durationType;
        this.startTs = startTs;
        this.endTs = endTs;
    }

    @Override
    public long getPeriod() {
        return 1L;
    }

    @Override
    public boolean isTime(ZonedDateTime nowTruncatedToHours) {
        return isValidDurationTime(nowTruncatedToHours) && atTime == nowTruncatedToHours.get(SECOND_OF_DAY);
    }

    boolean isValidDurationTime(ZonedDateTime nowTruncatedToHours) {
        if (durationType == ReportDurationType.CUSTOM) {
            long nowMillis = nowTruncatedToHours.toInstant().toEpochMilli();
            return startTs <= nowMillis && nowMillis <= endTs;
        }
        return true;
    }
}
