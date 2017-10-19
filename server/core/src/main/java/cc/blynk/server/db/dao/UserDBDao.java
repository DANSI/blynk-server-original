package cc.blynk.server.db.dao;

import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static cc.blynk.utils.DateTimeUtils.UTC_CALENDAR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.16.
 */
public class UserDBDao {

    private static final String upsertUser =
            "INSERT INTO users (email, appName, region, ip, name, pass, last_modified, last_logged,"
                    + " last_logged_ip, is_facebook_user, is_super_admin, energy, json) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (email, appName) DO UPDATE "
                    + "SET ip = EXCLUDED.ip, pass = EXCLUDED.pass, name = EXCLUDED.name, "
                    + "last_modified = EXCLUDED.last_modified, "
                    + "last_logged = EXCLUDED.last_logged, last_logged_ip = EXCLUDED.last_logged_ip, "
                    + "is_facebook_user = EXCLUDED.is_facebook_user, is_super_admin = EXCLUDED.is_super_admin, "
                    + "energy = EXCLUDED.energy, json = EXCLUDED.json, region = EXCLUDED.region";
    private static final String selectAllUsers = "SELECT * from users where region = ?";
    private static final String selectIpForUser = "SELECT ip FROM users WHERE email = ? AND appName = ?";
    private static final String deleteUser = "DELETE FROM users WHERE email = ? AND appName = ?";

    private static final Logger log = LogManager.getLogger(UserDBDao.class);
    private final HikariDataSource ds;

    public UserDBDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public int getDBVersion() throws Exception {
        int dbVersion;
        try (Connection connection = ds.getConnection();
             Statement statement = connection.createStatement()) {

            try (ResultSet rs = statement.executeQuery("SELECT current_setting('server_version_num')")) {
                rs.next();
                dbVersion = rs.getInt(1);
                connection.commit();
            }
        }
        return dbVersion;
    }

    public String getUserServerIp(String email, String appName) {
        String ip = null;

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectIpForUser)) {

            statement.setString(1, email);
            statement.setString(2, appName);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ip = rs.getString("ip");
                }
                connection.commit();
            }
        } catch (Exception e) {
            log.error("Error getting user server ip. {}-{}. Reason : {}", email, appName, e.getMessage());
        }

        return ip;
    }

    public ConcurrentMap<UserKey, User> getAllUsers(String region) throws Exception {
        ConcurrentMap<UserKey, User> users = new ConcurrentHashMap<>();

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectAllUsers)) {

            statement.setString(1, region);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    User user = new User(
                            rs.getString("email"),
                            rs.getString("pass"),
                            rs.getString("appName"),
                            rs.getString("region"),
                            rs.getString("ip"),
                            rs.getBoolean("is_facebook_user"),
                            rs.getBoolean("is_super_admin"),
                            rs.getString("name"),
                            getTs(rs, "last_modified"),
                            getTs(rs, "last_logged"),
                            rs.getString("last_logged_ip"),
                            JsonParser.parseProfileFromString(rs.getString("json")),
                            rs.getInt("energy")
                            );

                    users.put(new UserKey(user), user);
                }
                connection.commit();
            }
        }

        log.info("Loaded {} users.", users.size());

        return users;
    }

    private static long getTs(ResultSet rs, String fieldName) throws SQLException {
        Timestamp t = rs.getTimestamp(fieldName, UTC_CALENDAR);
        return t == null ? 0 : t.getTime();
    }

    public void save(ArrayList<User> users) {
        long start = System.currentTimeMillis();
        log.info("Storing users...");

        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement(upsertUser)) {

            for (User user : users) {
                ps.setString(1, user.email);
                ps.setString(2, user.appName);
                ps.setString(3, user.region);
                ps.setString(4, user.ip);
                ps.setString(5, user.name);
                ps.setString(6, user.pass);
                ps.setTimestamp(7, new Timestamp(user.lastModifiedTs), UTC_CALENDAR);
                ps.setTimestamp(8, new Timestamp(user.lastLoggedAt), UTC_CALENDAR);
                ps.setString(9, user.lastLoggedIP); //finish
                ps.setBoolean(10, user.isFacebookUser);
                ps.setBoolean(11, user.isSuperAdmin);
                ps.setInt(12, user.energy);
                ps.setString(13, user.profile.toString());
                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
        } catch (Exception e) {
            log.error("Error upserting users in DB.", e);
        }
        log.info("Storing users finished. Time {}. Users saved {}", System.currentTimeMillis() - start, users.size());
    }

    public boolean deleteUser(UserKey userKey) {
        int removed = 0;

        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement(deleteUser)) {

            ps.setString(1, userKey.email);
            ps.setString(2, userKey.appName);

            removed = ps.executeUpdate();

            connection.commit();
        } catch (Exception e) {
            log.error("Error removing user {} from DB.", userKey, e);
        }

        return removed > 0;
    }
}
