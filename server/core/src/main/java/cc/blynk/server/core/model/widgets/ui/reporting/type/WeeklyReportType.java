package cc.blynk.server.core.model.widgets.ui.reporting.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
public class WeeklyReportType extends DailyReportType {

    public final int dayOfTheWeek;

    @JsonCreator
    public WeeklyReportType(@JsonProperty("atTime") long atTime,
                            @JsonProperty("durationType") ReportDurationType durationType,
                            @JsonProperty("startTs") long startTs,
                            @JsonProperty("endTs") long endTs,
                            @JsonProperty("dayOfTheWeek") int dayOfTheWeek) {
        super(atTime, durationType, startTs, endTs);
        this.dayOfTheWeek = dayOfTheWeek;
    }
}
