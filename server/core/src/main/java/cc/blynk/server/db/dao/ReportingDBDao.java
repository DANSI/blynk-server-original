package cc.blynk.server.db.dao;

import cc.blynk.server.core.model.enums.GraphType;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.reporting.average.AggregationKey;
import cc.blynk.server.core.reporting.average.AggregationValue;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.16.
 */
public class ReportingDBDao {

    public static final String insertMinute = "INSERT INTO reporting_average_minute VALUES (?, ?, ?, ?, ?, ?)";
    public static final String insertHourly = "INSERT INTO reporting_average_hourly VALUES (?, ?, ?, ?, ?, ?)";
    public static final String insertDaily = "INSERT INTO reporting_average_daily VALUES (?, ?, ?, ?, ?, ?)";
    public static final String selectMinute = "SELECT ts, value FROM reporting_average_minute WHERE ts > ? ORDER BY ts DESC limit ?";
    public static final String selectHourly = "SELECT ts, value FROM reporting_average_hourly WHERE ts > ? ORDER BY ts DESC limit ?";
    public static final String selectDaily = "SELECT ts, value FROM reporting_average_daily WHERE ts > ? ORDER BY ts DESC limit ?";
    public static final String deleteMinute = "DELETE FROM reporting_average_minute WHERE ts < ?";
    public static final String deleteHour = "DELETE FROM reporting_average_hourly WHERE ts < ?";
    public static final String deleteDaily = "DELETE FROM reporting_average_daily WHERE ts < ?";
    private static final Logger log = LogManager.getLogger(ReportingDBDao.class);
    private final HikariDataSource ds;

    public ReportingDBDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public static void prepareReportingSelect(PreparedStatement ps, long ts, int limit) throws SQLException {
        ps.setLong(1, ts);
        ps.setInt(2, limit);
    }

    private static void prepareReportingInsert(PreparedStatement ps,
                                               Map.Entry<AggregationKey, AggregationValue> entry,
                                               GraphType type) throws SQLException {
        final AggregationKey key = entry.getKey();
        final AggregationValue value = entry.getValue();
        prepareReportingInsert(ps, key.username, key.dashId, key.pin, key.pinType, key.getTs(type), value.calcAverage());
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

    private static String getTableByGraphType(GraphType graphType) {
        switch (graphType) {
            case MINUTE :
                return insertMinute;
            case HOURLY :
                return insertHourly;
            default :
                return insertDaily;
        }
    }

    public void insert(Map<AggregationKey, AggregationValue> map, GraphType graphType) {
        long start = System.currentTimeMillis();

        log.info("Storing {} reporting...", graphType.name());

        String insertSQL = getTableByGraphType(graphType);

        try (Connection connection = ds.getConnection();
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

        log.info("Storing {} reporting finished. Time {}. Records saved {}", graphType.name(), System.currentTimeMillis() - start, map.size());
    }

    public void cleanOldReportingRecords(Instant now) {
        log.info("Removing old reporting records...");

        int minuteRecordsRemoved = 0;
        int hourRecordsRemoved = 0;

        try (Connection connection = ds.getConnection();
             PreparedStatement psMinute = connection.prepareStatement(deleteMinute);
             PreparedStatement psHour = connection.prepareStatement(deleteHour)) {

            psMinute.setLong(1, now.minus(360 + 1, ChronoUnit.MINUTES).toEpochMilli());
            psHour.setLong(1, now.minus(168 + 1, ChronoUnit.HOURS).toEpochMilli());

            minuteRecordsRemoved = psMinute.executeUpdate();
            hourRecordsRemoved = psHour.executeUpdate();

            connection.commit();
        } catch (Exception e) {
            log.error("Error inserting reporting data in DB.", e);
        }
        log.info("Removing finished. Minute records {}, hour records {}. Time {}",
                minuteRecordsRemoved, hourRecordsRemoved, System.currentTimeMillis() - now.toEpochMilli());
    }

}
