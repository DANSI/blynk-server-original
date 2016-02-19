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
    private static final Logger log = LogManager.getLogger(DBManager.class);
    private final HikariDataSource ds;

    public DBManager(ServerProperties serverProperties) {
        HikariConfig config = new HikariConfig(serverProperties);
        config.setAutoCommit(false);
        config.setConnectionTimeout(5000);

        log.info("DB url : {}", config.getJdbcUrl());
        log.info("DB user : {}", config.getUsername());
        log.info("Connecting to DB...");
        this.ds = new HikariDataSource(config);
        log.info("Connected to database successfully.");
    }

    public static void prepareReportingInsert(PreparedStatement preparedStatement,
                                              String username,
                                              int dashId,
                                              byte pin,
                                              char pinType,
                                              long ts,
                                              double value) throws SQLException {
        preparedStatement.setString(1, username);
        preparedStatement.setInt(2, dashId);
        preparedStatement.setByte(3, pin);
        preparedStatement.setString(4, String.valueOf(pinType));
        preparedStatement.setLong(5, ts);
        preparedStatement.setDouble(6, value);
    }

    public Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    public void close() {
        ds.close();
    }

}
