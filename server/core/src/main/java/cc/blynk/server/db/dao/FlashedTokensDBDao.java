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

    private static final String selectToken = "SELECT * from flashed_tokens where token = ?";
    private static final String activateToken =
            "UPDATE flashed_tokens SET is_activated = true, ts = NOW() WHERE token = ?";
    private static final String insertToken =
            "INSERT INTO flashed_tokens (token, app_name, email, project_id, device_id) values (?, ?, ?, ?, ?)";

    private static final Logger log = LogManager.getLogger(FlashedTokensDBDao.class);
    private final HikariDataSource ds;

    public FlashedTokensDBDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public FlashedToken selectFlashedToken(String token) {
        log.info("Select flashed token {}.", token);

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectToken)) {

            statement.setString(1, token);

            try (ResultSet rs = statement.executeQuery()) {
                connection.commit();

                if (rs.next()) {
                    return new FlashedToken(rs.getString("token"), rs.getString("app_name"),
                            rs.getString("email"), rs.getInt("project_id"), rs.getInt("device_id"),
                            rs.getBoolean("is_activated"), rs.getDate("ts")
                    );
                }
            }
        } catch (Exception e) {
            log.error("Error getting flashed token.", e);
        }

        return null;
    }

    public boolean activateFlashedToken(String token) {
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(activateToken)) {

            statement.setString(1, token);
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
        ps.setString(2, flashedToken.appId);
        ps.setString(3, flashedToken.email);
        ps.setInt(4, flashedToken.dashId);
        ps.setInt(5, flashedToken.deviceId);
    }
}
