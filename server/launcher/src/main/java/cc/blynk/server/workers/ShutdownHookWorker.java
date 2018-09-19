package cc.blynk.server.workers;

import cc.blynk.server.Holder;
import cc.blynk.server.servers.BaseServer;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Used to close and store all important info to disk.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.03.15.
 */
public class ShutdownHookWorker implements Runnable {

    private final BaseServer[] servers;
    private final Holder holder;
    private final ProfileSaverWorker profileSaverWorker;
    private final ScheduledExecutorService scheduler;

    public ShutdownHookWorker(BaseServer[] servers, Holder holder,
                              ScheduledExecutorService scheduler,
                              ProfileSaverWorker profileSaverWorker) {
        this.servers = servers;
        this.holder = holder;
        this.profileSaverWorker = profileSaverWorker;
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        System.out.println("Catch shutdown hook.");
        System.out.println("Stopping servers...");

        for (var server : servers) {
            try {
                server.close().sync();
            } catch (Throwable t) {
                System.out.println("Error on server shutdown : " + t.getCause());
            }
        }

        System.out.println("Stopping scheduler...");
        scheduler.shutdown();

        try {
            holder.close();
        } catch (Exception e) {
            System.out.println("Error stopping holder...");
        }

        System.out.println("Saving user profiles...");
        profileSaverWorker.close();

        System.out.println("Done.");
    }

}
