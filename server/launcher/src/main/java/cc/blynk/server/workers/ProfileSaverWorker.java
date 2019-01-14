package cc.blynk.server.workers;

import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.db.DBManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Background thread that once a minute stores all user DB to disk in case profile was changed since last saving.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/12/2015.
 */
public class ProfileSaverWorker implements Runnable, Closeable {

    private static final Logger log = LogManager.getLogger(ProfileSaverWorker.class);

    //1 min
    private final UserDao userDao;
    private final FileManager fileManager;
    private final DBManager dbManager;
    private long lastStart;
    private long backupTs;

    public ProfileSaverWorker(UserDao userDao, FileManager fileManager, DBManager dbManager) {
        this.userDao = userDao;
        this.fileManager = fileManager;
        this.dbManager = dbManager;
        this.lastStart = System.currentTimeMillis();
        this.backupTs = 0;
    }

    @Override
    public void run() {
        try {
            log.debug("Starting saving user db.");

            final long now = System.currentTimeMillis();

            ArrayList<User> users = saveModified();

            dbManager.saveUsers(users);

            //backup only for local mode
            if (dbManager.dbIsNotEnabled() && users.size() > 0) {
                archiveUser(now);
            }

            lastStart = now;

            log.debug("Saving user db finished. Modified {} users.", users.size());
        } catch (Throwable t) {
            log.error("Error saving users.", t);
        }
    }

    private void archiveUser(long now) {
        if (now - backupTs > 86_400_000) {
            //it is time for backup, once per day.
            log.info("Backup for user DB started...");
            backupTs = now;
            for (User user : userDao.users.values()) {
                try {
                    Path path = fileManager.generateBackupFileName(user.email, user.appName);
                    JsonParser.writeUser(path.toFile(), user);
                } catch (Exception e) {
                    //ignore
                }
            }
            log.info("Backup for user DB finished.");
        }
    }

    private ArrayList<User> saveModified() {
        var users = new ArrayList<User>();

        for (User user : userDao.getUsers().values()) {
            if (user.isUpdated(lastStart)) {
                try {
                    fileManager.overrideUserFile(user);
                    users.add(user);
                } catch (Exception e) {
                    log.error("Error saving : {}.", user);
                }
            }
        }

        return users;
    }

    @Override
    public void close() {
        run();
    }
}
