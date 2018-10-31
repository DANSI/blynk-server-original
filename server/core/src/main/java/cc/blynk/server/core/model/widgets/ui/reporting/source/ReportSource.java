package cc.blynk.server.core.model.widgets.ui.reporting.source;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.DeviceCleaner;
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
        @JsonSubTypes.Type(value = TileTemplateReportSource.class, name = "TILE_TEMPLATE"),
        @JsonSubTypes.Type(value = DeviceReportSource.class, name = "DEVICE")
})
public abstract class ReportSource implements DeviceCleaner {

    public final ReportDataStream[] reportDataStreams;

    ReportSource(ReportDataStream[] reportDataStreams) {
        this.reportDataStreams = reportDataStreams == null ? EMPTY_REPORT_DATA_STREAMS : reportDataStreams;
    }

    public abstract int[] getDeviceIds();

    public boolean isValid() {
        return reportDataStreams.length > 0;
    }

    public boolean isSame(short pin, PinType pinType) {
        for (ReportDataStream reportDataStream : reportDataStreams) {
            if (reportDataStream.isSame(pin, pinType)) {
                return true;
            }
        }
        return false;
    }
}
