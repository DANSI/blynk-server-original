package cc.blynk.server.workers;

import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.db.DBManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public ProfileSaverWorker(UserDao userDao, FileManager fileManager, DBManager dbManager) {
        this.userDao = userDao;
        this.fileManager = fileManager;
        this.dbManager = dbManager;
        this.lastStart = System.currentTimeMillis();
    }

    private static boolean isUpdated(long lastStart, User user) {
        return (lastStart <= user.lastModifiedTs) || isDashUpdated(lastStart, user);
    }

    private static boolean isDashUpdated(long lastStart, User user) {
        for (DashBoard dashBoard : user.profile.dashBoards) {
            if (lastStart <= dashBoard.updatedAt) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run() {
        try {
            log.debug("Starting saving user db.");

            long newStart = System.currentTimeMillis();

            List<User> users = saveModified();

            dbManager.saveUsers(users);

            lastStart = newStart;

            log.debug("Saving user db finished. Modified {} users.", users.size());
        } catch (Throwable t) {
            log.error("Error saving users.", t);
        }
    }

    private List<User> saveModified() {
        List<User> users = new ArrayList<>();

        for (User user : userDao.getUsers().values()) {
            if (isUpdated(lastStart, user)) {
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

    public List<User> saveAll() {
        List<User> users = new ArrayList<>(userDao.getUsers().size());

        for (User user : userDao.getUsers().values()) {
            try {
                fileManager.overrideUserFile(user);
                users.add(user);
            } catch (IOException e) {
                log.error("Error saving : {}.", user);
            }
        }

        return users;
    }

    @Override
    public void close() {
        run();
    }
}
