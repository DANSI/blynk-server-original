package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportScheduler;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportDataStream;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.notifications.mail.MailWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31/05/2018.
 *
 */
public class ReportTask implements Runnable {

    private static final Logger log = LogManager.getLogger(ReportTask.class);

    private final User user;

    private final int dashId;

    private final int reportId;

    private final Report report;

    private final ReportScheduler reportScheduler;

    private final MailWrapper mailWrapper;

    private final ReportingDao reportingDao;

    ReportTask(User user, int dashId, Report report,
               ReportScheduler reportScheduler, MailWrapper mailWrapper, ReportingDao reportingDao) {
        this.user = user;
        this.dashId = dashId;
        this.reportId = report.id;
        this.report = report;
        this.reportScheduler = reportScheduler;
        this.mailWrapper = mailWrapper;
        this.reportingDao = reportingDao;
    }

    ReportTask(User user, int dashId, Report report) {
        this(user, dashId, report, null, null, null);
    }

    @Override
    public void run() {
        try {
            long now = System.currentTimeMillis();
            int fetchCount = (int) report.reportType.getFetchCount(report.granularityType);

            for (ReportSource reportSource : report.reportSources) {
                if (reportSource.isValid()) {
                    for (int deviceId : reportSource.getDeviceIds()) {
                        for (ReportDataStream reportDataStream : reportSource.reportDataStreams) {
                            reportingDao.getByteBufferFromDisk(user, dashId, deviceId, reportDataStream.pinType,
                                    reportDataStream.pin, fetchCount, report.granularityType, 0);
                        }
                    }
                }
            }

            mailWrapper.sendText(report.recipients, report.name, "Your report is ready.");
            long newNow = System.currentTimeMillis();

            log.info("Processed report for {}, time {} ms.", user.email, newNow - now);
            log.debug(report);

            report.lastReportAt = newNow;
            long initialDelaySeconds = report.calculateDelayInSeconds();
            report.nextReportAt = newNow + initialDelaySeconds * 1000;

            //rescheduling report
            log.info("Rescheduling report for {} with delay {}.",
                    user.email, initialDelaySeconds);
            reportScheduler.schedule(this, initialDelaySeconds, TimeUnit.SECONDS);
        } catch (IllegalCommandException ice) {
            log.info("Seems like report is expired for {}.", user.email);
            report.nextReportAt = -1L;
        } catch (Exception e) {
            log.debug("Error generating report {} for {}.", report, user.email, e);
        }
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
        return dashId == that.dashId
                && reportId == that.reportId
                && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, dashId, reportId);
    }
}
