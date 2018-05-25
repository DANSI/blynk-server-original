package cc.blynk.server.core.model.widgets.ui.reporting.type;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OneTimeReportType.class, name = "ONE_TIME"),
        @JsonSubTypes.Type(value = DailyReportType.class, name = "DAILY"),
        @JsonSubTypes.Type(value = WeeklyReportType.class, name = "WEEKLY"),
        @JsonSubTypes.Type(value = MonthlyReportType.class, name = "MONTHLY")
})
public abstract class BaseReportType {

    public abstract boolean isTime(ZonedDateTime nowTruncatedToHours);

    public long reportPeriodMillis() {
        return TimeUnit.DAYS.toMillis(getPeriod());
    }

    public abstract long getPeriod();

}
