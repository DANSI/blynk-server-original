package cc.blynk.server.workers;

import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportDataStream;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.HOURS;

/**
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.01.18.
 */
public class ReportGeneratorWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(ReportGeneratorWorker.class);

    private final UserDao userDao;
    private final ReportingDao reportingDao;

    public ReportGeneratorWorker(UserDao userDao, ReportingDao reportingDao) {
        this.userDao = userDao;
        this.reportingDao = reportingDao;
    }

    @Override
    public void run() {
        try {
            log.info("Start generating reports...");

            long now = System.currentTimeMillis();
            //todo
            //actually, it is better to do not save data for such pins
            //but fow now this approach is simpler and quicker
            int result = generateReports();

            log.info("{} reports generated.", result, System.currentTimeMillis() - now);
        } catch (Throwable t) {
            log.error("Error generating reports.", t);
        }
    }

    private int generateReports() {
        int reportsCounter = 0;
        ZonedDateTime currentDateTime = ZonedDateTime.now(DateTimeUtils.UTC).truncatedTo(HOURS);
        for (User user : userDao.getUsers().values()) {
            for (DashBoard dash : user.profile.dashBoards) {
                ReportingWidget reportingWidget = dash.getReportingWidget();
                if (reportingWidget != null) {
                    Report[] reports = reportingWidget.reports;
                    if (reports != null && reports.length > 0) {
                        for (Report report : reports) {
                            if (report.isValid() && report.isTime(currentDateTime)) {
                                sendReport(user, dash, report);
                                report.lastProcessedAt = currentDateTime.toInstant().toEpochMilli();
                            }
                        }
                    }
                }
            }
        }
        return reportsCounter;
    }

    private void sendReport(User user, DashBoard dash, Report report) {
        long fetchCount = pointsNumber(report.granularityType, report.reportType.getPeriod());
        for (ReportSource reportSource : report.reportSources) {
            if (reportSource.isValid()) {
                for (int deviceId : reportSource.getDeviceIds()) {
                    for (ReportDataStream reportDataStream : reportSource.reportDataStreams) {
                        if (reportDataStream.isSelected) {

                        }
                    }
                }
            }
        }

    }

    public long pointsNumber(GraphGranularityType graphGranularityType, long duration) {
        switch (graphGranularityType) {
            case DAILY:
                return TimeUnit.DAYS.toDays(duration);
            case HOURLY:
                return TimeUnit.DAYS.toHours(duration);
            default:
                return TimeUnit.DAYS.toMinutes(duration);
        }
    }
}
