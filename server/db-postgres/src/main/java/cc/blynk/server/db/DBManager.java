package cc.blynk.server.db;

import cc.blynk.utils.ServerProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.02.16.
 */
public class DBManager {

    public static final String insertMinute = "INSERT INTO reporting_average_minute VALUES (?, ?, ?, ?, ?, ?)";
    public static final String insertHourly = "INSERT INTO reporting_average_hourly VALUES (?, ?, ?, ?, ?, ?)";
    public static final String insertDaily = "INSERT INTO reporting_average_daily VALUES (?, ?, ?, ?, ?, ?)";

    public static final String selectMinute = "SELECT ts, value FROM reporting_average_minute WHERE ts > ? ORDER BY ts DESC limit ?";
    public static final String selectHourly = "SELECT ts, value FROM reporting_average_hourly WHERE ts > ? ORDER BY ts DESC limit ?";
    public static final String selectDaily = "SELECT ts, value FROM reporting_average_daily WHERE ts > ? ORDER BY ts DESC limit ?";

    private static final Logger log = LogManager.getLogger(DBManager.class);
    private final HikariDataSource ds;

    public DBManager(ServerProperties serverProperties) {
        HikariConfig config = new HikariConfig(serverProperties);
        config.setAutoCommit(false);
        config.setConnectionTimeout(5000);

        log.info("DB host : {}", serverProperties.getProperty("dataSource.serverName"));
        log.info("DB name : {}", serverProperties.getProperty("dataSource.databaseName"));
        log.info("DB user : {}", serverProperties.getProperty("dataSource.user"));
        log.info("Connecting to DB...");
        this.ds = new HikariDataSource(config);
        log.info("Connected to database successfully.");
    }

    public static void prepareReportingInsert(PreparedStatement ps,
                                              String username,
                                              int dashId,
                                              byte pin,
                                              char pinType,
                                              long ts,
                                              double value) throws SQLException {
        ps.setString(1, username);
        ps.setInt(2, dashId);
        ps.setByte(3, pin);
        ps.setString(4, String.valueOf(pinType));
        ps.setLong(5, ts);
        ps.setDouble(6, value);
    }

    public static void prepareReportingSelect(PreparedStatement ps, long ts, int limit) throws SQLException {
        ps.setLong(1, ts);
        ps.setInt(2, limit);
    }

    public Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    public void close() {
        ds.close();
    }

}
