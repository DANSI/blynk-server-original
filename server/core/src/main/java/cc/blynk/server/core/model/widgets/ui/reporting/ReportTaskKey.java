package cc.blynk.server.core.model.widgets.ui.reporting;

import cc.blynk.server.core.model.auth.User;

import java.util.Objects;

public class ReportTaskKey {

    public final User user;

    public final int dashId;

    public final int reportId;

    public ReportTaskKey(User user, int dashId, int reportId) {
        this.user = user;
        this.dashId = dashId;
        this.reportId = reportId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReportTaskKey that = (ReportTaskKey) o;
        return dashId == that.dashId
                && reportId == that.reportId
                && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, dashId, reportId);
    }
}
