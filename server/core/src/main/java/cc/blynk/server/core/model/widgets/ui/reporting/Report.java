package cc.blynk.server.core.model.widgets.ui.reporting;

import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.others.rtc.StringToZoneId;
import cc.blynk.server.core.model.widgets.others.rtc.ZoneIdToString;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.server.core.model.widgets.ui.reporting.type.BaseReportType;
import cc.blynk.server.core.model.widgets.ui.reporting.type.DailyReport;
import cc.blynk.server.core.model.widgets.ui.reporting.type.OneTimeReport;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.internal.EmptyArraysUtil;
import cc.blynk.utils.validators.BlynkEmailValidator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static cc.blynk.server.core.model.widgets.ui.reporting.ReportOutput.CSV_FILE_PER_DEVICE_PER_PIN;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
public class Report {

    public final int id;

    public final String name;

    public final ReportSource[] reportSources;

    public final BaseReportType reportType;

    public final String recipients;

    public final GraphGranularityType granularityType;

    public final boolean isActive;

    public final ReportOutput reportOutput;

    public final Format format;

    @JsonSerialize(using = ZoneIdToString.class)
    @JsonDeserialize(using = StringToZoneId.class, as = ZoneId.class)
    public final ZoneId tzName;

    public volatile long nextReportAt;

    public volatile long lastReportAt;

    public volatile ReportResult lastRunResult;

    @JsonCreator
    public Report(@JsonProperty("id") int id,
                  @JsonProperty("name") String name,
                  @JsonProperty("reportSources") ReportSource[] reportSources,
                  @JsonProperty("reportType") BaseReportType reportType,
                  @JsonProperty("recipients") String recipients,
                  @JsonProperty("granularityType") GraphGranularityType granularityType,
                  @JsonProperty("isActive") boolean isActive,
                  @JsonProperty("reportOutput") ReportOutput reportOutput,
                  @JsonProperty("format") Format format,
                  @JsonProperty("tzName") ZoneId tzName,
                  @JsonProperty("nextReportAt") long nextReportAt,
                  @JsonProperty("lastReportAt") long lastReportAt,
                  @JsonProperty("lastRunResult") ReportResult lastRunResult) {
        this.id = id;
        this.name = name;
        this.reportSources = reportSources == null ? EmptyArraysUtil.EMPTY_REPORT_SOURCES : reportSources;
        this.reportType = reportType;
        this.recipients = recipients;
        this.granularityType = granularityType == null ? GraphGranularityType.MINUTE : granularityType;
        this.isActive = isActive;
        this.reportOutput = reportOutput == null ? CSV_FILE_PER_DEVICE_PER_PIN : reportOutput;
        this.format = format;
        this.tzName = tzName;
        this.nextReportAt = nextReportAt;
        this.lastReportAt = lastReportAt;
        this.lastRunResult = lastRunResult;
    }

    public boolean isValid() {
        return reportType != null && reportType.isValid()
                && reportSources != null && reportSources.length > 0
                && BlynkEmailValidator.isValidEmails(recipients);
    }

    public boolean isPeriodic() {
        return !(reportType instanceof OneTimeReport);
    }

    public static int getPrice() {
        return 2900;
    }

    public long calculateDelayInSeconds() throws IllegalCommandBodyException {
        DailyReport basePeriodicReportType = (DailyReport) reportType;

        ZonedDateTime zonedNow = ZonedDateTime.now(tzName);
        ZonedDateTime zonedStartAt = basePeriodicReportType.getNextTriggerTime(zonedNow, tzName);
        if (basePeriodicReportType.isExpired(zonedStartAt, tzName)) {
            throw new IllegalCommandBodyException("Report is expired.");
        }

        Duration duration = Duration.between(zonedNow, zonedStartAt);
        long initialDelaySeconds = duration.getSeconds();

        if (initialDelaySeconds < 0) {
            throw new IllegalCommandBodyException("Initial delay in less than zero.");
        }

        return initialDelaySeconds;
    }

    String buildDynamicSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("Report name: ").append(getReportName()).append("<br>");
        reportType.buildDynamicSection(sb, tzName);
        return sb.toString();
    }

    public String getReportName() {
        return (name == null || name.isEmpty()) ? "Report" : name;
    }

    public DateTimeFormatter makeFormatter() {
        return (format == null || format == Format.TS)
                ? null
                : DateTimeFormatter.ofPattern(format.pattern).withZone(tzName);
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }
}
