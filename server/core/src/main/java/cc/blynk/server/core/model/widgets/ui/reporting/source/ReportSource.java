package cc.blynk.server.core.model.widgets.ui.reporting.source;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_REPORT_DATA_STREAMS;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TileTemplateReportSource.class, name = "TILE_TEMPLATE")
})
public abstract class ReportSource {

    public final ReportDataStream[] reportDataStreams;

    ReportSource(ReportDataStream[] reportDataStreams) {
        this.reportDataStreams = reportDataStreams == null ? EMPTY_REPORT_DATA_STREAMS : reportDataStreams;
    }
}
