package cc.blynk.server;

import cc.blynk.server.core.BaseServer;
import cc.blynk.server.storage.reporting.average.AverageAggregator;
import cc.blynk.server.workers.ProfileSaverWorker;
import cc.blynk.server.workers.ShutdownHookWorker;
import cc.blynk.server.workers.StatsWorker;
import cc.blynk.server.workers.StorageWorker;
import cc.blynk.server.workers.timer.TimerWorker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static cc.blynk.server.utils.ReportingUtil.getReportingFolder;

/**
 * Launches a bunch of separate jobs/schedulers responsible for different aspects of business logic
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.09.15.
 */
public class JobLauncher {

    public static void start(Holder holder, BaseServer... servers) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        long startDelay;

        StorageWorker storageWorker = new StorageWorker(holder.averageAggregator, getReportingFolder(holder.props.getProperty("data.folder")));

        //to start at the beggining of an hour
        startDelay = AverageAggregator.HOURS - (System.currentTimeMillis() % AverageAggregator.HOURS);
        scheduler.scheduleAtFixedRate(storageWorker, startDelay, AverageAggregator.HOURS, TimeUnit.MILLISECONDS);

        ProfileSaverWorker profileSaverWorker = new ProfileSaverWorker(holder.userRegistry, holder.fileManager);
        scheduler.scheduleAtFixedRate(profileSaverWorker, 1000,
                holder.props.getIntProperty("profile.save.worker.period"), TimeUnit.MILLISECONDS);

        StatsWorker statsWorker = new StatsWorker(holder.stats, holder.sessionsHolder, holder.userRegistry);
        scheduler.scheduleAtFixedRate(statsWorker, 1000,
                holder.props.getIntProperty("stats.print.worker.period"), TimeUnit.MILLISECONDS);

        //millis we need to wait to start scheduler at the beginning of a second.
        startDelay = 1000 - (System.currentTimeMillis() % 1000);

        //separate thread for timer.
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
                new TimerWorker(holder.userRegistry, holder.sessionsHolder), startDelay, 1000, TimeUnit.MILLISECONDS);

        //shutdown hook thread catcher
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHookWorker(holder.averageAggregator, profileSaverWorker,
                holder.notificationsProcessor, servers)));
    }


}
