package cc.blynk.server.core.model.widgets.ui.reporting.type;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OneTimeReport.class, name = "ONE_TIME"),
        @JsonSubTypes.Type(value = DailyReport.class, name = "DAILY"),
        @JsonSubTypes.Type(value = WeeklyReport.class, name = "WEEKLY"),
        @JsonSubTypes.Type(value = MonthlyReport.class, name = "MONTHLY")
})
public abstract class BaseReportType {

    public abstract ZonedDateTime getNextTriggerTime(ZonedDateTime zonedNow, ZoneId zoneId);

    public abstract boolean isValid();

}
