package cc.blynk.server.core.model.widgets.ui.reporting.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
public class OneTimeReport extends BaseReportType {

    public final long rangeMillis;

    @JsonCreator
    public OneTimeReport(@JsonProperty("rangeMillis") long rangeMillis) {
        this.rangeMillis = rangeMillis;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public ZonedDateTime getNextTriggerTime(ZonedDateTime zonedNow, ZoneId zoneId) {
        return zonedNow;
    }
}
