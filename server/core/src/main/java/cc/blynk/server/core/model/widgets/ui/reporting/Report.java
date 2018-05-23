package cc.blynk.server.core.model.widgets.ui.reporting;

import cc.blynk.server.core.model.widgets.others.rtc.StringToZoneId;
import cc.blynk.server.core.model.widgets.others.rtc.ZoneIdToString;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.server.core.model.widgets.ui.reporting.type.BaseReportType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.ZoneId;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
public class Report {

    public final String name;

    public final ReportSource[] reportSources;

    public final BaseReportType reportType;

    public final String recipients;

    public final GraphGranularityType granularityType;

    public final boolean isActive;

    @JsonSerialize(using = ZoneIdToString.class)
    @JsonDeserialize(using = StringToZoneId.class, as = ZoneId.class)
    public final ZoneId tzName;

    @JsonCreator
    public Report(@JsonProperty("name") String name,
                  @JsonProperty("reportSources") ReportSource[] reportSources,
                  @JsonProperty("reportType") BaseReportType reportType,
                  @JsonProperty("recipients") String recipients,
                  @JsonProperty("granularityType") GraphGranularityType granularityType,
                  @JsonProperty("isActive") boolean isActive,
                  @JsonProperty("tzName") ZoneId tzName) {
        this.name = name;
        this.reportSources = reportSources;
        this.reportType = reportType;
        this.recipients = recipients;
        this.granularityType = granularityType;
        this.isActive = isActive;
        this.tzName = tzName;
    }
}
