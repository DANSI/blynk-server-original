package cc.blynk.server.db;

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
public class RedeemDBDao {

    public static final String selectRedeemToken = "SELECT * from redeem where token = ?";
    public static final String updateRedeemToken = "UPDATE redeem SET username = ?, version = 2, isRedeemed = true WHERE token = ? and version = 1";
    private static final Logger log = LogManager.getLogger(RedeemDBDao.class);
    private final HikariDataSource ds;

    public RedeemDBDao(HikariDataSource ds) {
        this.ds = ds;
    }

    protected Redeem selectRedeemByToken(String token) throws Exception {
        log.info("Redeem select for {}", token);

        ResultSet rs = null;
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectRedeemToken)) {

            statement.setString(1, token);
            rs = statement.executeQuery();
            connection.commit();

            if (rs.next()) {
                return new Redeem(rs.getString("token"), rs.getString("company"),
                        rs.getBoolean("isRedeemed"), rs.getString("username"),
                        rs.getInt("reward"), rs.getInt("version")
                );
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }

        return null;
    }

    protected boolean updateRedeem(String username, String token) throws Exception {
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(updateRedeemToken)) {

            statement.setString(1, username);
            statement.setString(2, token);
            int updatedRows = statement.executeUpdate();
            connection.commit();
            return updatedRows == 1;
        }
    }
}
