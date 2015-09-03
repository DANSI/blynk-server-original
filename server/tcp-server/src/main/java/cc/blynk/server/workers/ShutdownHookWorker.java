package cc.blynk.server.workers;

import cc.blynk.server.core.BaseServer;
import cc.blynk.server.storage.reporting.average.AverageAggregator;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Used to close and store all important info to disk.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.03.15.
 */
public class ShutdownHookWorker implements Runnable {

    private final static Logger log = LogManager.getLogger(ShutdownHookWorker.class);

    private final AverageAggregator averageAggregator;
    private final BaseServer[] servers;
    private final ProfileSaverWorker profileSaverWorker;
    private final NotificationsProcessor notificationsProcessor;

    public ShutdownHookWorker(AverageAggregator averageAggregator, ProfileSaverWorker profileSaverWorker, NotificationsProcessor notificationsProcessor,
                              BaseServer... servers) {
        this.averageAggregator = averageAggregator;
        this.servers = servers;
        this.profileSaverWorker = profileSaverWorker;
        this.notificationsProcessor = notificationsProcessor;
    }

    @Override
    public void run() {
        log.info("Catch shutdown hook. Trying to save users and close threads.");

        notificationsProcessor.stop();
        profileSaverWorker.run();

        for (BaseServer server : servers) {
            server.stop();
        }

        averageAggregator.close();
    }

}
