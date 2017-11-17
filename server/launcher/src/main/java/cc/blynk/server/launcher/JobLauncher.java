package cc.blynk.server.launcher;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.reporting.average.AverageAggregatorProcessor;
import cc.blynk.server.internal.ReportingUtil;
import cc.blynk.server.workers.CertificateRenewalWorker;
import cc.blynk.server.workers.ProfileSaverWorker;
import cc.blynk.server.workers.ReportingWorker;
import cc.blynk.server.workers.ShutdownHookWorker;
import cc.blynk.server.workers.StatsWorker;
import cc.blynk.utils.structure.LRUCache;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        long startDelay;

        ReportingWorker reportingWorker = new ReportingWorker(
                holder.reportingDao,
                ReportingUtil.getReportingFolder(holder.props.getProperty("data.folder")),
                holder.dbManager
        );

        //to start at the beggining of an minute
        startDelay = AverageAggregatorProcessor.MINUTE
                - (System.currentTimeMillis() % AverageAggregatorProcessor.MINUTE);
        scheduler.scheduleAtFixedRate(reportingWorker, startDelay,
                AverageAggregatorProcessor.MINUTE, MILLISECONDS);

        ProfileSaverWorker profileSaverWorker =
                new ProfileSaverWorker(holder.userDao, holder.fileManager, holder.dbManager);

        //running 1 sec later after reporting
        scheduler.scheduleAtFixedRate(profileSaverWorker, startDelay + 1000,
                holder.props.getIntProperty("profile.save.worker.period"), MILLISECONDS);

        StatsWorker statsWorker = new StatsWorker(holder);
        scheduler.scheduleAtFixedRate(statsWorker, 1000,
                holder.props.getIntProperty("stats.print.worker.period"), MILLISECONDS);

        if (holder.sslContextHolder.isAutoGenerationEnabled) {
            scheduler.scheduleAtFixedRate(
                    new CertificateRenewalWorker(holder.sslContextHolder.acmeClient, 21), 1, 1, TimeUnit.DAYS
            );
        }
        scheduler.scheduleAtFixedRate(LRUCache.LOGIN_TOKENS_CACHE::clear, 1, 1, HOURS);

        //millis we need to wait to start scheduler at the beginning of a second.
        startDelay = 1000 - (System.currentTimeMillis() % 1000);

        //separate thread for timer and reading widgets
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ses.scheduleAtFixedRate(holder.timerWorker, startDelay, 1000, MILLISECONDS);
        ses.scheduleAtFixedRate(holder.readingWidgetsWorker, startDelay + 400, 1000, MILLISECONDS);

        //shutdown hook thread catcher
        Runtime.getRuntime().addShutdownHook(new Thread(
                new ShutdownHookWorker(servers, holder, scheduler, profileSaverWorker)
        ));
    }


}
