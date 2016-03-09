package cc.blynk.server.db;

import cc.blynk.server.core.model.auth.User;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.16.
 */
public class UserDBDao {

    public static final String upsertUser = "INSERT INTO users VALUES (?, ?, ?) ON CONFLICT (username) DO UPDATE SET json = EXCLUDED.json, region = EXCLUDED.region";
    private static final Logger log = LogManager.getLogger(UserDBDao.class);
    private final HikariDataSource ds;

    public UserDBDao(HikariDataSource ds) {
        this.ds = ds;
    }

    protected void save(List<User> users) {
        long start = System.currentTimeMillis();
        log.info("Storing users...");

        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement(upsertUser)) {

            for (User user : users) {
                ps.setString(1, user.name);
                ps.setString(2, null);
                ps.setString(3, user.toString());
                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
        } catch (Exception e) {
            log.error("Error upserting users in DB.", e);
        }
        log.info("Storing users finished. Time {}. Users saved {}", System.currentTimeMillis() - start, users.size());
    }
}
