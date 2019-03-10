package cc.blynk.server.core.model.widgets.ui.reporting;

import cc.blynk.server.core.model.auth.User;

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

        if (dashId != that.dashId) {
            return false;
        }
        if (reportId != that.reportId) {
            return false;
        }
        return user != null ? user.equals(that.user) : that.user == null;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + dashId;
        result = 31 * result + reportId;
        return result;
    }
}
