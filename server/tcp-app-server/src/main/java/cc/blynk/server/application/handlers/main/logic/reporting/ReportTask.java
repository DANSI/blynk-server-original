package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportScheduler;
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

    private final String email;

    private final String appName;

    private final int reportId;

    private final Report report;

    private final ReportScheduler reportScheduler;

    private final MailWrapper mailWrapper;

    ReportTask(String email, String appName, Report report,
                      ReportScheduler reportScheduler, MailWrapper mailWrapper) {
        this.email = email;
        this.appName = appName;
        this.reportId = report.id;
        this.report = report;
        this.reportScheduler = reportScheduler;
        this.mailWrapper = mailWrapper;
    }

    ReportTask(String email, String appName, Report report) {
        this(email, appName, report, null, null);
    }

    @Override
    public void run() {
        try {
            long now = System.currentTimeMillis();
            mailWrapper.sendText(report.recipients, report.name, "Your report is ready.");
            long newNow = System.currentTimeMillis();
            log.info("Processed report for {}, time {} ms.",
                    this.email, newNow - now);
            log.debug(report);

            report.lastReportAt = newNow;
            long initialDelaySeconds = report.calculateDelayInSeconds();
            report.nextReportAt = newNow + initialDelaySeconds * 1000;

            //rescheduling report
            log.info("Rescheduling report for {} with delay {}.",
                    this.email, initialDelaySeconds);
            reportScheduler.schedule(this, initialDelaySeconds, TimeUnit.SECONDS);
        } catch (IllegalCommandException ice) {
            log.info("Seems like report is expired for {}.", email);
            report.nextReportAt = -1L;
        } catch (Exception e) {
            log.debug("Error generating report {} for {}.", report, email, e);
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
        return reportId == that.reportId
                && Objects.equals(email, that.email)
                && Objects.equals(appName, that.appName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, appName, reportId);
    }
}
