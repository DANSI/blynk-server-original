package cc.blynk.server.core.model.widgets.ui.reporting.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
}
