package cc.blynk.server.core.model.widgets.ui.reporting;

import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.utils.BlynkTPFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static cc.blynk.server.core.model.widgets.ui.reporting.ReportResult.EXPIRED;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31/05/2018.
 *
 */
public class ReportScheduler extends ScheduledThreadPoolExecutor {

    private static final Logger log = LogManager.getLogger(ReportScheduler.class);

    public final Map<ReportTaskKey, ScheduledFuture<?>> map;
    public final MailWrapper mailWrapper;
    public final ReportingDiskDao reportingDao;
    public final String downloadUrl;

    public ReportScheduler(int corePoolSize, String downloadUrl,
                           MailWrapper mailWrapper, ReportingDiskDao reportingDao, Map<UserKey, User> users) {
        super(corePoolSize,  BlynkTPFactory.build("report"));
        setRemoveOnCancelPolicy(true);
        setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.map = new ConcurrentHashMap<>();
        this.downloadUrl = downloadUrl;
        this.mailWrapper = mailWrapper;
        this.reportingDao = reportingDao;
        init(users);
    }

    private void init(Map<UserKey, User> users) {
        int counter = 0;
        for (Map.Entry<UserKey, User> entry : users.entrySet()) {
            User user = entry.getValue();
            for (DashBoard dashBoard : user.profile.dashBoards) {
                for (Widget widget : dashBoard.widgets) {
                    if (widget instanceof ReportingWidget) {
                        ReportingWidget reportingWidget = (ReportingWidget) widget;
                        for (Report report : reportingWidget.reports) {
                            if (report.isValid() && report.isPeriodic() && report.isActive) {
                                try {
                                    long now = System.currentTimeMillis();
                                    long initialDelaySeconds;

                                    if (report.nextReportAt < now && report.lastRunResult != EXPIRED) {
                                        //this is special case, when we restart server we may miss some reports
                                        //while the server is down, so we perform checks and run those reports,
                                        //so we are sure we didn't miss any report.
                                        log.warn("Rescheduling missed report {} for {}.", report, user.email);
                                        initialDelaySeconds = 0;
                                    } else {
                                        initialDelaySeconds = report.calculateDelayInSeconds();
                                        log.trace("Adding periodic report for user {} with delay {} to scheduler.",
                                                user.email, initialDelaySeconds);
                                        report.nextReportAt = now + initialDelaySeconds * 1000;
                                    }
                                    schedule(user, dashBoard.id, report, initialDelaySeconds);
                                    counter++;
                                } catch (IllegalCommandBodyException e) {
                                    report.lastRunResult = EXPIRED;
                                    log.debug("Report is expired for {}, {}", user.email, report.id);
                                } catch (Exception e) {
                                    report.lastRunResult = ReportResult.ERROR;
                                    log.debug("Error scheduling report for {}, {}", user.email, report.id);
                                }
                            }
                        }
                    }
                }
            }
        }
        log.info("Reports : {}", counter);
    }

    public void schedule(User user, int dashId, Report report, long delayInSeconds) {
        schedule(
                new PeriodicReportTask(user, dashId, report, this),
                delayInSeconds,
                TimeUnit.SECONDS
        );
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        ScheduledFuture<?> scheduledFuture = super.schedule(task, delay, unit);
        if (task instanceof PeriodicReportTask) {
            BaseReportTask baseReportTask = (PeriodicReportTask) task;
            map.put(baseReportTask.key, scheduledFuture);
        }
        return scheduledFuture;
    }

    public void cancelStoredFuture(User user, int dashId) {
        Iterator<Map.Entry<ReportTaskKey, ScheduledFuture<?>>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<ReportTaskKey, ScheduledFuture<?>> entry = iter.next();
            ReportTaskKey reportTaskKey = entry.getKey();
            if (reportTaskKey.dashId == dashId && reportTaskKey.user.equals(user)) {
                iter.remove();
                ScheduledFuture<?> scheduledFuture = entry.getValue();
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(true);
                }
            }
        }
    }

    public boolean cancelStoredFuture(User user, int dashId, int reportId) {
        ReportTaskKey key = new ReportTaskKey(user, dashId, reportId);
        ScheduledFuture<?> scheduledFuture = map.remove(key);
        if (scheduledFuture == null) {
            return false;
        }
        return scheduledFuture.cancel(true);
    }

}
