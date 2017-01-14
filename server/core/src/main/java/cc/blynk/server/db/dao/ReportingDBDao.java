package cc.blynk.server.db.dao;

import cc.blynk.server.core.model.enums.GraphType;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.reporting.average.AggregationKey;
import cc.blynk.server.core.reporting.average.AggregationValue;
import cc.blynk.server.core.reporting.average.AverageAggregator;
import cc.blynk.server.core.stats.model.CommandStat;
import cc.blynk.server.core.stats.model.Stat;
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

    public static final String insertMinute = "INSERT INTO reporting_average_minute (username, project_id, device_id, pin, pinType, ts, value) VALUES (?, ?, ?, ?, ?, ?, ?)";
    public static final String insertHourly = "INSERT INTO reporting_average_hourly (username, project_id, device_id, pin, pinType, ts, value) VALUES (?, ?, ?, ?, ?, ?, ?)";
    public static final String insertDaily = "INSERT INTO reporting_average_daily (username, project_id, device_id, pin, pinType, ts, value) VALUES (?, ?, ?, ?, ?, ?, ?)";

    public static final String selectMinute = "SELECT ts, value FROM reporting_average_minute WHERE ts > ? ORDER BY ts DESC limit ?";
    public static final String selectHourly = "SELECT ts, value FROM reporting_average_hourly WHERE ts > ? ORDER BY ts DESC limit ?";
    public static final String selectDaily = "SELECT ts, value FROM reporting_average_daily WHERE ts > ? ORDER BY ts DESC limit ?";

    public static final String deleteMinute = "DELETE FROM reporting_average_minute WHERE ts < ?";
    public static final String deleteHour = "DELETE FROM reporting_average_hourly WHERE ts < ?";
    public static final String deleteDaily = "DELETE FROM reporting_average_daily WHERE ts < ?";

    public static final String insertStatMinute = "INSERT INTO reporting_app_stat_minute (region, ts, active, active_week, active_month, minute_rate, connected, online_apps, online_hards, total_online_apps, total_online_hards, registrations) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
    public static final String insertStatCommandsMinute = "INSERT INTO reporting_app_command_stat_minute (region, ts, response, register, login, load_profile, app_sync, sharing, get_token, ping, activate, deactivate, refresh_token, get_graph_data, export_graph_data, set_widget_property, bridge, hardware, get_share_dash, get_share_token, refresh_share_token, share_login, create_project, update_project, delete_project, hardware_sync, internal, sms, tweet, email, push, add_push_token, create_widget, update_widget, delete_widget, create_device, update_device, delete_device, get_devices, add_energy, get_energy, get_server, connect_redirect, web_sockets, eventor, webhooks, appTotal, hardTotal) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

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
        prepareReportingInsert(ps, key.username, key.dashId, key.deviceId, key.pin, PinType.getPinType(key.pinType), key.getTs(type), value.calcAverage());
    }

    public static void prepareReportingInsert(PreparedStatement ps,
                                                 String username,
                                                 int dashId,
                                                 int deviceId,
                                                 byte pin,
                                                 PinType pinType,
                                                 long ts,
                                                 double value) throws SQLException {
        ps.setString(1, username);
        ps.setInt(2, dashId);
        ps.setInt(3, deviceId);
        ps.setByte(4, pin);
        ps.setString(5, pinType.pinTypeString);
        ps.setLong(6, ts);
        ps.setDouble(7, value);
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

    public void insertStat(String region, Stat stat) {
        final long ts = (stat.ts / AverageAggregator.MINUTE) * AverageAggregator.MINUTE;

        try (Connection connection = ds.getConnection();
             PreparedStatement appStatPS = connection.prepareStatement(insertStatMinute);
             PreparedStatement commandStatPS = connection.prepareStatement(insertStatCommandsMinute)) {

            appStatPS.setString(1, region);
            appStatPS.setLong(2, ts);
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

            final CommandStat cs = stat.commands;
            commandStatPS.setString(1, region);
            commandStatPS.setLong(2, ts);
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
            commandStatPS.setInt(40, cs.addEnergy);
            commandStatPS.setInt(41, cs.getEnergy);
            commandStatPS.setInt(42, cs.getServer);
            commandStatPS.setInt(43, cs.connectRedirect);
            commandStatPS.setInt(44, cs.webSockets);
            commandStatPS.setInt(45, cs.eventor);
            commandStatPS.setInt(46, cs.webhooks);
            commandStatPS.setInt(47, cs.appTotal);
            commandStatPS.setInt(48, cs.hardTotal);
            commandStatPS.executeUpdate();

            connection.commit();
        } catch (Exception e) {
            log.error("Error inserting real time stat in DB.", e);
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

