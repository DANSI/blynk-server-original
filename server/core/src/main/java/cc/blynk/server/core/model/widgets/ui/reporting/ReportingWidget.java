package cc.blynk.server.core.model.widgets.ui.reporting;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.DeviceCleaner;
import cc.blynk.server.core.model.widgets.NoPinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.ButtonStyle;
import cc.blynk.server.core.model.widgets.controls.Edge;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;

import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_REPORTS;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_REPORT_SOURCES;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
public class ReportingWidget extends NoPinWidget implements DeviceCleaner {

    public ReportSource[] reportSources = EMPTY_REPORT_SOURCES;

    public boolean allowEndUserToDeleteDataOn;

    public FontSize fontSize = FontSize.MEDIUM;

    public Edge edge = Edge.ROUNDED;

    public ButtonStyle buttonStyle = ButtonStyle.SOLID;

    public int textColor;

    public volatile Report[] reports = EMPTY_REPORTS;

    public void validateId(int id) {
        Report report = getReportById(id);
        if (report != null) {
            throw new IllegalCommandException("Report with passed id already exists.");
        }
    }

    public Report getReportById(int id) {
        for (Report report : reports) {
            if (report.id == id) {
                return report;
            }
        }
        return null;
    }

    public int getReportIndexById(int id) {
        for (int i = 0; i < reports.length; i++) {
            if (id == reports[i].id) {
                return i;
            }
        }
        return -1;
    }

    public boolean hasPin(short pin, PinType pinType) {
        for (ReportSource reportSource : reportSources) {
            if (reportSource.isSame(pin, pinType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void deleteDevice(int deviceId) {
        for (Report report : reports) {
            if (report.reportSources != null) {
                for (ReportSource reportSource : report.reportSources) {
                    reportSource.deleteDevice(deviceId);
                }
            }
        }
    }

    @Override
    public int getPrice() {
        return Report.getPrice() * reports.length;
    }

    @Override
    public void erase() {
        this.reports = EMPTY_REPORTS;
    }

    @Override
    public void updateValue(Widget oldWidget) {
        if (oldWidget instanceof ReportingWidget) {
            this.reports = ((ReportingWidget) oldWidget).reports;
        }
    }
}
