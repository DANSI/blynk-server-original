package cc.blynk.server.workers;

import cc.blynk.server.dao.FileManager;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.model.auth.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Background thread that once a minute stores all user DB to disk in case profile was changed since last saving.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/12/2015.
 */
public class ProfileSaverWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(ProfileSaverWorker.class);

    //1 min
    private final UserRegistry userRegistry;
    private final FileManager fileManager;
    private long lastStart;

    public ProfileSaverWorker(UserRegistry userRegistry, FileManager fileManager) {
        this.userRegistry = userRegistry;
        this.fileManager = fileManager;
        this.lastStart = System.currentTimeMillis();
    }

    @Override
    public void run() {
        log.debug("Starting saving user db.");
        int count = 0;
        long newStart = System.currentTimeMillis();

        for (User user : userRegistry.getUsers().values()) {
            if (lastStart <= user.lastModifiedTs) {
                try {
                    fileManager.overrideUserFile(user);
                    count++;
                } catch (IOException e) {
                    log.error("Error saving : {}.", user);
                }
            }
        }

        lastStart = newStart;

        log.debug("Saving user db finished. Modified {} users.", count);
    }


}
