package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.core.model.widgets.ui.reporting.Report;

import java.util.Objects;

public abstract class ReportTask implements Runnable {

    final String email;

    final String appName;

    final Report report;

    public ReportTask(String email, String appName, Report report) {
        this.email = email;
        this.appName = appName;
        this.report = report;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReportTask that = (ReportTask) o;
        return report.id == that.report.id
                && Objects.equals(email, that.email)
                && Objects.equals(appName, that.appName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, appName, report.id);
    }
}
