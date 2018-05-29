package cc.blynk.server.core.model.widgets.ui.reporting;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportDataStream;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.server.core.model.widgets.ui.reporting.source.TileTemplateReportSource;
import cc.blynk.server.core.model.widgets.ui.reporting.type.DailyReportType;
import cc.blynk.server.core.model.widgets.ui.reporting.type.MonthlyReportType;
import cc.blynk.server.core.model.widgets.ui.reporting.type.OneTimeReportType;
import cc.blynk.server.core.model.widgets.ui.reporting.type.ReportDurationType;
import cc.blynk.server.core.model.widgets.ui.reporting.type.WeeklyReportType;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.Test;

import java.time.ZoneId;

import static cc.blynk.server.core.model.widgets.ui.reporting.ReportOutput.CSV_FILE_PER_DEVICE_PER_PIN;

public class ReportingModelTest {

    private ObjectWriter ow = JsonParser.init().writerWithDefaultPrettyPrinter().forType(ReportingWidget.class);

    @Test
    public void printModel() throws Exception {
        ReportDataStream reportDataStream = new ReportDataStream((byte) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                null
                );

        ReportSource reportSource2 = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0, 1}
        );

        Report report = new Report(1, "My One Time Report",
                new ReportSource[] {reportSource2},
                new OneTimeReportType(86400), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN, ZoneId.of("UTC"));

        Report report2 = new Report(2, "My Daily Report",
                new ReportSource[] {reportSource2},
                new DailyReportType(60, ReportDurationType.CUSTOM, 100, 200), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN, ZoneId.of("UTC"));

        Report report3 = new Report(3, "My Daily Report",
                new ReportSource[] {reportSource2},
                new WeeklyReportType(60, ReportDurationType.CUSTOM, 100, 200, 1), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN, ZoneId.of("UTC"));

        Report report4 = new Report(4, "My Daily Report",
                new ReportSource[] {reportSource2},
                new MonthlyReportType(60, ReportDurationType.CUSTOM, 100, 200, 1), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN, ZoneId.of("UTC"));

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };
        reportingWidget.reports = new Report[] {
                report,
                report2,
                report3,
                report4
        };


        System.out.println(ow.writeValueAsString(reportingWidget));
    }

}
