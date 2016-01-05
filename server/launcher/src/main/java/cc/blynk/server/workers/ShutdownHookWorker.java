package cc.blynk.server.workers;

import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.reporting.average.AverageAggregator;

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

    public ShutdownHookWorker(AverageAggregator averageAggregator, ProfileSaverWorker profileSaverWorker, BlockingIOProcessor blockingIOProcessor,
                              BaseServer... servers) {
        this.averageAggregator = averageAggregator;
        this.servers = servers;
        this.profileSaverWorker = profileSaverWorker;
        this.blockingIOProcessor = blockingIOProcessor;
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

        System.out.println("Stopping aggregator...");
        averageAggregator.close();
        System.out.println("Done.");
    }

}
