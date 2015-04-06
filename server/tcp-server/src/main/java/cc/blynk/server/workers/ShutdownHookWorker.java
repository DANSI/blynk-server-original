package cc.blynk.server.workers;

import cc.blynk.server.core.BaseServer;
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

    private final BaseServer hardServer;
    private final BaseServer appServer;
    private final ProfileSaverWorker profileSaverWorker;
    private final NotificationsProcessor notificationsProcessor;

    public ShutdownHookWorker(BaseServer hardServer, BaseServer appServer, ProfileSaverWorker profileSaverWorker,
                              NotificationsProcessor notificationsProcessor) {
        this.hardServer = hardServer;
        this.appServer = appServer;
        this.profileSaverWorker = profileSaverWorker;
        this.notificationsProcessor = notificationsProcessor;
    }

    @Override
    public void run() {
        log.info("Catch shutdown hook. Trying to save users and close threads.");

        notificationsProcessor.stop();
        profileSaverWorker.run();

        if (hardServer != null) {
            hardServer.stop();
        }

        if (appServer != null) {
            appServer.stop();
        }
    }

}
