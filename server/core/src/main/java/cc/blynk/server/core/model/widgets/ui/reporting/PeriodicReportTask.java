package cc.blynk.server.core.model.widgets.ui.reporting;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;

import java.util.concurrent.TimeUnit;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31/05/2018.
 *
 */
public class PeriodicReportTask extends BaseReportTask {

    private final ReportScheduler reportScheduler;

    PeriodicReportTask(User user, int dashId, Report report, ReportScheduler reportScheduler) {
        super(user, dashId, report,
                reportScheduler.mailWrapper, reportScheduler.reportingDao,
                reportScheduler.downloadUrl);
        this.reportScheduler = reportScheduler;
    }

    @Override
    public void run() {
        try {
            long finishedAt = generateReport();
            report.lastReportAt = finishedAt;
            reschedule(finishedAt);
            log.debug("After rescheduling: {}", report);
        } catch (IllegalCommandBodyException ice) {
            log.info("Seems like report is expired for {}.", key.user.email);
            report.lastRunResult = ReportResult.EXPIRED;
        } catch (Exception e) {
            log.debug("Error generating report {} for {}.", report, key.user.email, e);
        }
    }

    private void reschedule(long reportFinishedAt) {
        long initialDelaySeconds = report.calculateDelayInSeconds();
        report.nextReportAt = reportFinishedAt + initialDelaySeconds * 1000;

        //rescheduling report
        log.info("Rescheduling report for {} with delay {}.", key.user.email, initialDelaySeconds);
        reportScheduler.schedule(this, initialDelaySeconds, TimeUnit.SECONDS);
    }
}
