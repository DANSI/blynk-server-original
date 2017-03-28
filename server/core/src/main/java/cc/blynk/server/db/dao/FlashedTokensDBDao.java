package cc.blynk.server.db.dao;

import cc.blynk.server.db.model.FlashedToken;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.16.
 */
public class FlashedTokensDBDao {

    public static final String selectToken = "SELECT * from flashed_tokens where token = ? and app_name = ?";
    public static final String activateToken = "UPDATE flashed_tokens SET is_activated = true, ts = NOW() WHERE token = ? and app_name = ?";
    public static final String insertToken = "INSERT INTO flashed_tokens (token, app_name, device_id) values (?, ?, ?)";

    private static final Logger log = LogManager.getLogger(FlashedTokensDBDao.class);
    private final HikariDataSource ds;

    public FlashedTokensDBDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public FlashedToken selectFlashedToken(String token, String appName) {
        log.info("Select flashed {}, app {}", token, appName);

        ResultSet rs = null;
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectToken)) {

            statement.setString(1, token);
            statement.setString(2, appName);
            rs = statement.executeQuery();
            connection.commit();

            if (rs.next()) {
                return new FlashedToken(rs.getString("token"), rs.getString("app_name"),
                        rs.getString("email"), rs.getInt("device_id"),
                        rs.getBoolean("is_activated"), rs.getDate("ts")
                );
            }
        } catch (Exception e) {
            log.error("Error getting flashed token.", e);
        } finally {
            if (rs != null) {
                 try {
                     rs.close();
                 } catch (Exception e) {
                     //ignore
                 }
            }
        }

        return null;
    }

    public boolean activateFlashedToken(String token, String appName) {
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(activateToken)) {

            statement.setString(1, token);
            statement.setString(2, appName);
            int updatedRows = statement.executeUpdate();
            connection.commit();
            return updatedRows == 1;
        } catch (Exception e) {
            return false;
        }
    }

    public void insertFlashedTokens(FlashedToken[] flashedTokenList) throws Exception {
        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertToken)) {

            for (FlashedToken flashedToken : flashedTokenList) {
                insert(ps, flashedToken);
                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
        }
    }

    private static void insert(PreparedStatement ps, FlashedToken flashedToken) throws Exception {
        ps.setString(1, flashedToken.token);
        ps.setString(2, flashedToken.appName);
        ps.setInt(3, flashedToken.deviceId);
    }
}
