package cc.blynk.server.db.dao;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphPeriod;
import cc.blynk.server.core.reporting.average.AggregationKey;
import cc.blynk.server.core.reporting.average.AggregationValue;
import cc.blynk.server.core.reporting.average.AverageAggregatorProcessor;
import cc.blynk.server.core.stats.model.CommandStat;
import cc.blynk.server.core.stats.model.HttpStat;
import cc.blynk.server.core.stats.model.Stat;
import cc.blynk.utils.DateTimeUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.16.
 */
public class ReportingDBDao {

    public static final String insertMinute =
            "INSERT INTO reporting_average_minute (email, project_id, device_id, pin, pin_type, ts, value) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String insertHourly =
            "INSERT INTO reporting_average_hourly (email, project_id, device_id, pin, pin_type, ts, value) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String insertDaily =
            "INSERT INTO reporting_average_daily (email, project_id, device_id, pin, pin_type, ts, value) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String insertRawData =
            "INSERT INTO reporting_raw_data (email, project_id, device_id, pin, pinType, ts, "
                    + "stringValue, doubleValue) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    public static final String selectMinute =
            "SELECT ts, value FROM reporting_average_minute WHERE ts > ? ORDER BY ts DESC limit ?";
    public static final String selectHourly =
            "SELECT ts, value FROM reporting_average_hourly WHERE ts > ? ORDER BY ts DESC limit ?";
    public static final String selectDaily =
            "SELECT ts, value FROM reporting_average_daily WHERE ts > ? ORDER BY ts DESC limit ?";

    private static final String deleteMinute = "DELETE FROM reporting_average_minute WHERE ts < ?";
    private static final String deleteHour = "DELETE FROM reporting_average_hourly WHERE ts < ?";
    public static final String deleteDaily = "DELETE FROM reporting_average_daily WHERE ts < ?";

    private static final String insertStatMinute =
            "INSERT INTO reporting_app_stat_minute (region, ts, active, active_week, active_month, "
                    + "minute_rate, connected, online_apps, online_hards, "
                    + "total_online_apps, total_online_hards, registrations) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String insertStatCommandsMinute =
            "INSERT INTO reporting_app_command_stat_minute (region, ts, response, register, "
                    + "login, load_profile, app_sync, sharing, get_token, ping, activate, "
                    + "deactivate, refresh_token, get_graph_data, export_graph_data, "
                    + "set_widget_property, bridge, hardware, get_share_dash, get_share_token, "
                    + "refresh_share_token, share_login, create_project, update_project, "
                    + "delete_project, hardware_sync, internal, sms, tweet, email, push, "
                    + "add_push_token, create_widget, update_widget, delete_widget, create_device, "
                    + "update_device, delete_device, get_devices, create_tag, update_tag, "
                    + "delete_tag, get_tags, add_energy, get_energy, get_server, connect_redirect, "
                    + "web_sockets, eventor, webhooks, appTotal, hardTotal) "
                    + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?"
                    + ",?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    //todo add one more column mqttTotal and replace hardTotal with mqtt total
    private static final String insertStatHttpCommandMinute =
            "INSERT INTO reporting_http_command_stat_minute (region, ts, is_hardware_connected, "
                    + "is_app_connected, get_pin_data, update_pin, email, push, get_project, qr,"
                    + " get_history_pin_data, total) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final Logger log = LogManager.getLogger(ReportingDBDao.class);

    private final HikariDataSource ds;

    public ReportingDBDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public static void prepareReportingSelect(PreparedStatement ps, long ts, int limit) throws SQLException {
        ps.setTimestamp(1, new Timestamp(ts), DateTimeUtils.UTC_CALENDAR);
        ps.setInt(2, limit);
    }

    private static void prepareReportingInsert(PreparedStatement ps,
                                               Map.Entry<AggregationKey, AggregationValue> entry,
                                               GraphGranularityType type) throws SQLException {
        AggregationKey key = entry.getKey();
        AggregationValue value = entry.getValue();
        prepareReportingInsert(ps, key.getEmail(), key.getDashId(), key.getDeviceId(),
                key.getPin(), key.getPinType(), key.getTs(type), value.calcAverage());
    }

    public static void prepareReportingInsert(PreparedStatement ps,
                                                 String email,
                                                 int dashId,
                                                 int deviceId,
                                                 short pin,
                                                 PinType pinType,
                                                 long ts,
                                                 double value) throws SQLException {
        ps.setString(1, email);
        ps.setInt(2, dashId);
        ps.setInt(3, deviceId);
        ps.setShort(4, pin);
        ps.setInt(5, pinType.ordinal());
        ps.setTimestamp(6, new Timestamp(ts), DateTimeUtils.UTC_CALENDAR);
        ps.setDouble(7, value);
    }

    private static String getTableByGraphType(GraphGranularityType graphGranularityType) {
        switch (graphGranularityType) {
            case MINUTE :
                return insertMinute;
            case HOURLY :
                return insertHourly;
            default :
                return insertDaily;
        }
    }

    public void insertRawData(Map<AggregationKey, Object> rawData) {
        long start = System.currentTimeMillis();

        log.info("Storing raw reporting...");
        int counter = 0;

        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertRawData)) {

            for (Iterator<Map.Entry<AggregationKey, Object>> iter = rawData.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<AggregationKey, Object> entry = iter.next();

                final AggregationKey key = entry.getKey();
                final Object value = entry.getValue();

                ps.setString(1, key.getEmail());
                ps.setInt(2, key.getDashId());
                ps.setInt(3, key.getDeviceId());
                ps.setShort(4, key.getPin());
                ps.setString(5, key.getPinType().pinTypeString);
                ps.setTimestamp(6, new Timestamp(key.ts), DateTimeUtils.UTC_CALENDAR);

                if (value instanceof String) {
                    ps.setString(7, (String) value);
                    ps.setNull(8, Types.DOUBLE);
                } else {
                    ps.setNull(7, Types.VARCHAR);
                    ps.setDouble(8, (Double) value);
                }

                ps.addBatch();
                counter++;
                iter.remove();
            }

            ps.executeBatch();
            connection.commit();
        } catch (Exception e) {
            log.error("Error inserting raw reporting data in DB.", e);
        }

        log.info("Storing raw reporting finished. Time {}. Records saved {}",
                System.currentTimeMillis() - start, counter);
    }

    public void insertStat(String region, Stat stat) {
        final long ts = (stat.ts / AverageAggregatorProcessor.MINUTE) * AverageAggregatorProcessor.MINUTE;
        final Timestamp timestamp = new Timestamp(ts);

        try (Connection connection = ds.getConnection();
             PreparedStatement appStatPS = connection.prepareStatement(insertStatMinute);
             PreparedStatement commandStatPS = connection.prepareStatement(insertStatCommandsMinute);
             PreparedStatement httpStatPS = connection.prepareStatement(insertStatHttpCommandMinute)) {

            appStatPS.setString(1, region);
            appStatPS.setTimestamp(2, timestamp, DateTimeUtils.UTC_CALENDAR);
            appStatPS.setInt(3, stat.active);
            appStatPS.setInt(4, stat.activeWeek);
            appStatPS.setInt(5, stat.activeMonth);
            appStatPS.setInt(6, stat.oneMinRate);
            appStatPS.setInt(7, stat.connected);
            appStatPS.setInt(8, stat.onlineApps);
            appStatPS.setInt(9, stat.onlineHards);
            appStatPS.setInt(10, stat.totalOnlineApps);
            appStatPS.setInt(11, stat.totalOnlineHards);
            appStatPS.setInt(12, stat.registrations);
            appStatPS.executeUpdate();

            final HttpStat hs = stat.http;
            httpStatPS.setString(1, region);
            httpStatPS.setTimestamp(2, timestamp, DateTimeUtils.UTC_CALENDAR);
            httpStatPS.setInt(3, hs.isHardwareConnected);
            httpStatPS.setInt(4, hs.isAppConnected);
            httpStatPS.setInt(5, hs.getPinData);
            httpStatPS.setInt(6, hs.updatePinData);
            httpStatPS.setInt(7, hs.email);
            httpStatPS.setInt(8, hs.notify);
            httpStatPS.setInt(9, hs.getProject);
            httpStatPS.setInt(10, hs.qr);
            httpStatPS.setInt(11, hs.getHistoryPinData);
            httpStatPS.setInt(12, hs.total);

            httpStatPS.executeUpdate();

            final CommandStat cs = stat.commands;
            commandStatPS.setString(1, region);
            commandStatPS.setTimestamp(2, timestamp, DateTimeUtils.UTC_CALENDAR);
            commandStatPS.setInt(3, cs.response);
            commandStatPS.setInt(4, cs.register);
            commandStatPS.setInt(5, cs.login);
            commandStatPS.setInt(6, cs.loadProfile);
            commandStatPS.setInt(7, cs.appSync);
            commandStatPS.setInt(8, cs.sharing);
            commandStatPS.setInt(9, cs.getToken);
            commandStatPS.setInt(10, cs.ping);
            commandStatPS.setInt(11, cs.activate);
            commandStatPS.setInt(12, cs.deactivate);
            commandStatPS.setInt(13, cs.refreshToken);
            commandStatPS.setInt(14, cs.getGraphData);
            commandStatPS.setInt(15, cs.exportGraphData);
            commandStatPS.setInt(16, cs.setWidgetProperty);
            commandStatPS.setInt(17, cs.bridge);
            commandStatPS.setInt(18, cs.hardware);
            commandStatPS.setInt(19, cs.getSharedDash);
            commandStatPS.setInt(20, cs.getShareToken);
            commandStatPS.setInt(21, cs.refreshShareToken);
            commandStatPS.setInt(22, cs.shareLogin);
            commandStatPS.setInt(23, cs.createProject);
            commandStatPS.setInt(24, cs.updateProject);
            commandStatPS.setInt(25, cs.deleteProject);
            commandStatPS.setInt(26, cs.hardwareSync);
            commandStatPS.setInt(27, cs.internal);
            commandStatPS.setInt(28, cs.sms);
            commandStatPS.setInt(29, cs.tweet);
            commandStatPS.setInt(30, cs.email);
            commandStatPS.setInt(31, cs.push);
            commandStatPS.setInt(32, cs.addPushToken);
            commandStatPS.setInt(33, cs.createWidget);
            commandStatPS.setInt(34, cs.updateWidget);
            commandStatPS.setInt(35, cs.deleteWidget);
            commandStatPS.setInt(36, cs.createDevice);
            commandStatPS.setInt(37, cs.updateDevice);
            commandStatPS.setInt(38, cs.deleteDevice);
            commandStatPS.setInt(39, cs.getDevices);
            commandStatPS.setInt(40, cs.createTag);
            commandStatPS.setInt(41, cs.updateTag);
            commandStatPS.setInt(42, cs.deleteTag);
            commandStatPS.setInt(43, cs.getTags);
            commandStatPS.setInt(44, cs.addEnergy);
            commandStatPS.setInt(45, cs.getEnergy);
            commandStatPS.setInt(46, cs.getServer);
            commandStatPS.setInt(47, cs.connectRedirect);
            commandStatPS.setInt(48, cs.webSockets);
            commandStatPS.setInt(49, cs.eventor);
            commandStatPS.setInt(50, cs.webhooks);
            commandStatPS.setInt(51, cs.appTotal);
            commandStatPS.setInt(52, cs.mqttTotal);
            commandStatPS.executeUpdate();

            connection.commit();
        } catch (Exception e) {
            log.error("Error inserting real time stat in DB.", e);
        }
    }

    public void insert(Map<AggregationKey, AggregationValue> map, GraphGranularityType graphGranularityType) {
        long start = System.currentTimeMillis();

        log.info("Storing {} reporting...", graphGranularityType.name());

        String insertSQL = getTableByGraphType(graphGranularityType);

        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertSQL)) {

            for (Map.Entry<AggregationKey, AggregationValue> entry : map.entrySet()) {
                prepareReportingInsert(ps, entry, graphGranularityType);
                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
        } catch (Exception e) {
            log.error("Error inserting reporting data in DB.", e);
        }

        log.info("Storing {} reporting finished. Time {}. Records saved {}",
                graphGranularityType.name(), System.currentTimeMillis() - start, map.size());
    }

    public void cleanOldReportingRecords(Instant now) {
        log.info("Removing old reporting records...");

        int minuteRecordsRemoved = 0;
        int hourRecordsRemoved = 0;

        try (Connection connection = ds.getConnection();
             PreparedStatement psMinute = connection.prepareStatement(deleteMinute);
             PreparedStatement psHour = connection.prepareStatement(deleteHour)) {

            //for minute table we store only data for last 24 hours
            psMinute.setTimestamp(1, new Timestamp(now.minus(GraphPeriod.DAY.numberOfPoints + 1,
                    ChronoUnit.MINUTES).toEpochMilli()), DateTimeUtils.UTC_CALENDAR);

            //for hour table we store only data for last 3 months
            psHour.setTimestamp(1, new Timestamp(now.minus(GraphPeriod.THREE_MONTHS.numberOfPoints + 1,
                    ChronoUnit.HOURS).toEpochMilli()), DateTimeUtils.UTC_CALENDAR);

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

