package cc.blynk.server.workers;

import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.reporting.average.AverageAggregator;
import cc.blynk.server.db.DBManager;

/**
 * Used to close and store all important info to disk.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.03.15.
 */
public class ShutdownHookWorker implements Runnable {

    private final AverageAggregator averageAggregator;
    private final BaseServer[] servers;
    private final ProfileSaverWorker profileSaverWorker;
    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;

    public ShutdownHookWorker(AverageAggregator averageAggregator,
                              ProfileSaverWorker profileSaverWorker,
                              BlockingIOProcessor blockingIOProcessor,
                              DBManager dbManager,
                              BaseServer... servers) {
        this.averageAggregator = averageAggregator;
        this.profileSaverWorker = profileSaverWorker;
        this.blockingIOProcessor = blockingIOProcessor;
        this.dbManager = dbManager;
        this.servers = servers;
    }

    @Override
    public void run() {
        System.out.println("Catch shutdown hook.");
        System.out.println("Stopping BlockingIOProcessor...");
        blockingIOProcessor.stop();
        System.out.println("Saving user profiles...");
        profileSaverWorker.run();

        System.out.println("Stopping servers...");
        for (BaseServer server : servers) {
            server.stop();
        }

        if (dbManager.isDBEnabled()) {
            System.out.println("Closing DB...");
            dbManager.close();
        }

        System.out.println("Stopping aggregator...");
        averageAggregator.close();
        System.out.println("Done.");
    }

}
