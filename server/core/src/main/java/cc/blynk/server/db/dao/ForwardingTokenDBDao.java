package cc.blynk.server.db.dao;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.StringJoiner;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.16.
 */
public class ForwardingTokenDBDao {

    private static final String selectHostByToken = "SELECT host from forwarding_tokens where token = ?";
    private static final String insertTokenHostProject =
            "INSERT INTO forwarding_tokens (token, host, email, project_id, device_id) values (?, ?, ?, ?, ?)";
    private static final String deleteToken = "delete from forwarding_tokens where token IN ";

    private static final Logger log = LogManager.getLogger(ForwardingTokenDBDao.class);
    private final HikariDataSource ds;

    public ForwardingTokenDBDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public boolean insertTokenHostBatch(List<ForwardingTokenEntry> entries) {
        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertTokenHostProject)) {

            for (ForwardingTokenEntry entry : entries) {
                ps.setString(1, entry.token);
                ps.setString(2, entry.host);
                ps.setString(3, entry.email);
                ps.setInt(4, entry.dashId);
                ps.setInt(5, entry.deviceId);
                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
            return true;
        } catch (Exception e) {
            log.error("Error insert token host. Reason : {}", e.getMessage());
        }
        return false;
    }

    public boolean insertTokenHost(String token, String host, String email, int dashId, int deviceId) {
        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertTokenHostProject)) {

            ps.setString(1, token);
            ps.setString(2, host);
            ps.setString(3, email);
            ps.setInt(4, dashId);
            ps.setInt(5, deviceId);
            ps.executeUpdate();

            connection.commit();
            return true;
        } catch (Exception e) {
            log.error("Error insert token host. Reason : {}", e.getMessage());
        }
        return false;
    }

    public String selectHostByToken(String token) {
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectHostByToken)) {

            statement.setString(1, token);
            try (ResultSet rs = statement.executeQuery()) {
                connection.commit();
                if (rs.next()) {
                    return rs.getString("host");
                }
            }
        } catch (Exception e) {
            log.error("Error getting token host. Reason : {}", e.getMessage());
        }
        return null;
    }

    public boolean deleteToken(String... tokens) {
        String query = deleteToken + makeQuestionMarks(tokens.length);

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            for (int i = 1; i <= tokens.length; i++) {
                statement.setString(i, tokens[i - 1]);
            }

            statement.executeUpdate();
            connection.commit();
            return true;
        } catch (Exception e) {
            log.error("Error deleting token host. Reason : {}", e.getMessage());
        }
        return false;
    }

    private static String makeQuestionMarks(int count) {
        StringJoiner sj = new StringJoiner(",", "(", ")");
        for (int i = 0; i < count; i++) {
            sj.add("?");
        }
        return sj.toString();
    }


}
