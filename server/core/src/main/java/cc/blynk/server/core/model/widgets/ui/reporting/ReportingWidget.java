package cc.blynk.server.core.model.widgets.ui.reporting;

import cc.blynk.server.core.model.widgets.NoPinWidget;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;

import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_REPORTS;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_REPORT_SOURCES;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.05.18.
 */
public class ReportingWidget extends NoPinWidget {

    public ReportSource[] reportSources = EMPTY_REPORT_SOURCES;

    public boolean allowEndUserToDeleteDataOn;

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

    @Override
    public int getPrice() {
        return Report.getPrice() * reports.length;
    }
}
