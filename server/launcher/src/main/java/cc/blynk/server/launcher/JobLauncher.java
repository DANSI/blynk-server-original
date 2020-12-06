package cc.blynk.server.launcher;

import cc.blynk.server.Holder;
import cc.blynk.server.core.reporting.average.AverageAggregatorProcessor;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.workers.CertificateRenewalWorker;
import cc.blynk.server.workers.HistoryGraphUnusedPinDataCleanerWorker;
import cc.blynk.server.workers.ProfileSaverWorker;
import cc.blynk.server.workers.ReportingTruncateWorker;
import cc.blynk.server.workers.ReportingWorker;
import cc.blynk.server.workers.ShutdownHookWorker;
import cc.blynk.server.workers.StatsWorker;
import cc.blynk.utils.BlynkTPFactory;
import cc.blynk.utils.structure.LRUCache;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Launches a bunch of separate jobs/schedulers responsible for different aspects of business logic
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.09.15.
 */
final class JobLauncher {

    private JobLauncher() {
    }

    public static void start(Holder holder, BaseServer[] servers) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, BlynkTPFactory.build("DataSaver"));

        long startDelay;

        ReportingWorker reportingWorker = new ReportingWorker(
                holder.reportingDiskDao,
                holder.props.getReportingFolder(),
                holder.reportingDBManager
        );

        //to start at the beggining of an minute
        startDelay = AverageAggregatorProcessor.MINUTE
                - (System.currentTimeMillis() % AverageAggregatorProcessor.MINUTE);
        scheduler.scheduleAtFixedRate(reportingWorker, startDelay,
                AverageAggregatorProcessor.MINUTE, MILLISECONDS);

        var profileSaverWorker = new ProfileSaverWorker(holder.userDao, holder.fileManager, holder.dbManager);

        //running 1 sec later after reporting
        scheduler.scheduleAtFixedRate(profileSaverWorker, startDelay + 1000,
                holder.props.getIntProperty("profile.save.worker.period"), MILLISECONDS);

        var statsWorker = new StatsWorker(holder);
        scheduler.scheduleAtFixedRate(statsWorker, 1000,
                holder.props.getIntProperty("stats.print.worker.period"), MILLISECONDS);

        if (holder.sslContextHolder.runRenewalWorker()) {
            if (holder.props.isRenewalDisabled()) {
                System.out.println("Certificate renewal disabled.");
            } else {
                scheduler.scheduleAtFixedRate(
                        new CertificateRenewalWorker(holder.sslContextHolder), 1, 1, TimeUnit.DAYS
                );
            }
        }
        scheduler.scheduleAtFixedRate(LRUCache.LOGIN_TOKENS_CACHE::clear, 1, 1, HOURS);
        scheduler.scheduleAtFixedRate(holder.tokenManager::clearTemporaryTokens, 7, 1, DAYS);

        //running once every 3 day
        //todo could be removed?
        var reportingDataDiskCleaner =
                new HistoryGraphUnusedPinDataCleanerWorker(holder.userDao, holder.reportingDiskDao);
        //once every 7 days
        scheduler.scheduleAtFixedRate(reportingDataDiskCleaner, 1, 7, DAYS);

        ReportingTruncateWorker reportingTruncateWorker = new ReportingTruncateWorker(holder.reportingDiskDao,
                holder.limits.storeMinuteRecordDays, holder.limits.storeReportCSVDays);

        //once every week
        scheduler.scheduleAtFixedRate(reportingTruncateWorker, 1, 24 * 7, HOURS);

        //millis we need to wait to start scheduler at the beginning of a second.
        startDelay = 1000 - (System.currentTimeMillis() % 1000);

        //separate thread for timer and reading widgets
        var ses = Executors.newScheduledThreadPool(1, BlynkTPFactory.build("TimerAndReading"));
        ses.scheduleAtFixedRate(holder.timerWorker, startDelay, 1000, MILLISECONDS);
        ses.scheduleAtFixedRate(holder.readingWidgetsWorker, startDelay + 400, 1000, MILLISECONDS);

        //shutdown hook thread catcher
        Runtime.getRuntime().addShutdownHook(new Thread(
                new ShutdownHookWorker(servers, holder, scheduler, profileSaverWorker)
        ));
    }


}
