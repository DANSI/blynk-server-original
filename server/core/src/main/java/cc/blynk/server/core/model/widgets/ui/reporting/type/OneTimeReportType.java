package cc.blynk.server.core.model.widgets.ui.reporting.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
public class OneTimeReportType extends BaseReportType {

    public final long rangeMillis;

    @JsonCreator
    public OneTimeReportType(@JsonProperty("rangeMillis") long rangeMillis) {
        this.rangeMillis = rangeMillis;
    }

    @Override
    public long getPeriod() {
        return 0;
    }

    @Override
    public long reportPeriodMillis() {
        return rangeMillis;
    }

    @Override
    public boolean isTime(ZonedDateTime nowTruncatedToHours) {
        return false;
    }
}
