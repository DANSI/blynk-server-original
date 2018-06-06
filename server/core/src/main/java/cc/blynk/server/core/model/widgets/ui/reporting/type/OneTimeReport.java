package cc.blynk.server.core.model.widgets.ui.reporting.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
public class OneTimeReport extends BaseReportType {

    private final long rangeMillis;

    @JsonCreator
    public OneTimeReport(@JsonProperty("rangeMillis") long rangeMillis) {
        this.rangeMillis = rangeMillis;
    }

    @Override
    public boolean isValid() {
        return getDuration() > 0;
    }

    @Override
    public String getDurationLabel() {
        return "One time";
    }

    @Override
    public void buildDynamicSection(StringBuilder sb, ZoneId zoneId) {
        sb.append("Period: ").append(getDurationLabel());
    }

    @Override
    public long getDuration() {
        return rangeMillis / TimeUnit.DAYS.toMillis(1);
    }

    @Override
    public ZonedDateTime getNextTriggerTime(ZonedDateTime zonedNow, ZoneId zoneId) {
        return zonedNow;
    }
}
