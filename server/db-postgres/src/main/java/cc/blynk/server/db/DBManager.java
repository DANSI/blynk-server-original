package cc.blynk.server.db;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.GraphType;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.reporting.average.AggregationKey;
import cc.blynk.server.core.reporting.average.AggregationValue;
import cc.blynk.utils.ServerProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.02.16.
 */
public class DBManager {

    public static final String upsertUser = "INSERT INTO users VALUES (?, ?) ON CONFLICT (username) DO UPDATE SET json = EXCLUDED.json";

    public static final String insertMinute = "INSERT INTO reporting_average_minute VALUES (?, ?, ?, ?, ?, ?)";
    public static final String insertHourly = "INSERT INTO reporting_average_hourly VALUES (?, ?, ?, ?, ?, ?)";
    public static final String insertDaily = "INSERT INTO reporting_average_daily VALUES (?, ?, ?, ?, ?, ?)";
    public static final String selectMinute = "SELECT ts, value FROM reporting_average_minute WHERE ts > ? ORDER BY ts DESC limit ?";
    public static final String selectHourly = "SELECT ts, value FROM reporting_average_hourly WHERE ts > ? ORDER BY ts DESC limit ?";
    public static final String selectDaily = "SELECT ts, value FROM reporting_average_daily WHERE ts > ? ORDER BY ts DESC limit ?";
    private static final Logger log = LogManager.getLogger(DBManager.class);
    private static final String DB_PROPERTIES_FILENAME = "db.properties";
    private final HikariDataSource ds;

    public DBManager() {
        this(DB_PROPERTIES_FILENAME);
    }

    public DBManager(String propsFilename) {
        ServerProperties serverProperties;
        try {
            serverProperties = new ServerProperties(propsFilename);
            if (serverProperties.size() == 0) {
                throw new RuntimeException();
            }
        } catch (RuntimeException e) {
            log.warn("No {} file found. Separate DB storage disabled.", propsFilename);
            this.ds = null;
            return;
        }

        HikariConfig config = new HikariConfig(serverProperties);
        config.setAutoCommit(false);
        config.setConnectionTimeout(5000);
        config.setMaximumPoolSize(2);

        log.info("DB host : {}", serverProperties.getProperty("dataSource.serverName"));
        log.info("DB name : {}", serverProperties.getProperty("dataSource.databaseName"));
        log.info("DB user : {}", serverProperties.getProperty("dataSource.user"));
        log.info("Connecting to DB...");

        HikariDataSource hikariDataSource;
        try {
            hikariDataSource = new HikariDataSource(config);
        } catch (Exception e) {
            log.error("Not able connect to DB. Skipping.", e);
            this.ds = null;
            return;
        }
        this.ds = hikariDataSource;

        log.info("Connected to database successfully.");
    }

    private static String getSQL(GraphType graphType) {
        switch (graphType) {
            case MINUTE :
                return insertMinute;
            case HOURLY :
                return insertHourly;
            default :
                return insertDaily;
        }
    }

    private static void prepareReportingInsert(PreparedStatement ps,
                                              Map.Entry<AggregationKey, AggregationValue> entry,
                                              GraphType type) throws SQLException {
        final AggregationKey key = entry.getKey();
        final AggregationValue value = entry.getValue();
        prepareReportingInsert(ps, key.username, key.dashId, key.pin, key.pinType, key.ts * type.period, value.calcAverage());
    }

    public static void prepareReportingInsert(PreparedStatement ps,
                                              String username,
                                              int dashId,
                                              byte pin,
                                              PinType pinType,
                                              long ts,
                                              double value) throws SQLException {
        ps.setString(1, username);
        ps.setInt(2, dashId);
        ps.setByte(3, pin);
        ps.setString(4, pinType.pinTypeString);
        ps.setLong(5, ts);
        ps.setDouble(6, value);
    }

    public static void prepareReportingSelect(PreparedStatement ps, long ts, int limit) throws SQLException {
        ps.setLong(1, ts);
        ps.setInt(2, limit);
    }

    public void saveUsers(List<User> users) {
        long start = System.currentTimeMillis();

        if (!isDBEnabled() || users.size() == 0) {
            return;
        }
        log.info("Storing users...");

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(upsertUser)) {

            for (User user : users) {
                ps.setString(1, user.name);
                ps.setString(2, user.toString());
                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
        } catch (Exception e) {
            log.error("Error upserting users in DB.", e);
        }
        log.info("Storing users finished. Time {}. Users saved {}", System.currentTimeMillis() - start, users.size());
    }

    public void insertReporting(Map<AggregationKey, AggregationValue> map, GraphType graphType) {
        if (!isDBEnabled() || map.size() == 0) {
            return;
        }

        String insertSQL = getSQL(graphType);

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(insertSQL)) {

            for (Map.Entry<AggregationKey, AggregationValue> entry : map.entrySet()) {
                prepareReportingInsert(ps, entry, graphType);
                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
        } catch (Exception e) {
           log.error("Error inserting reporting data in DB.", e);
        }
    }

    public boolean isDBEnabled() {
        return ds != null;
    }

    public void executeSQL(String sql) throws Exception {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            connection.commit();
        }
    }

    public Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    public void close() {
        if (ds != null) {
            ds.close();
        }
    }

}
