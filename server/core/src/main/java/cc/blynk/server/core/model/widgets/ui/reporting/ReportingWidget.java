package cc.blynk.server.core.model.widgets.ui.reporting;

import cc.blynk.server.core.model.widgets.NoPinWidget;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;

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

    @Override
    public int getPrice() {
        return 4900;
    }
}
