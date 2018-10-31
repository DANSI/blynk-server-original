package cc.blynk.server.db;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportScheduler;
import cc.blynk.server.core.reporting.average.AggregationKey;
import cc.blynk.server.core.reporting.average.AggregationValue;
import cc.blynk.server.core.reporting.average.AverageAggregatorProcessor;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.core.stats.model.CommandStat;
import cc.blynk.server.core.stats.model.HttpStat;
import cc.blynk.server.core.stats.model.Stat;
import cc.blynk.server.db.dao.ReportingDBDao;
import cc.blynk.utils.AppNameUtil;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.02.16.
 */
public class RealtimeStatsDBTest {

    private static ReportingDBManager reportingDBManager;
    private static BlockingIOProcessor blockingIOProcessor;
    private static final Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    @BeforeClass
    public static void init() throws Exception {
        blockingIOProcessor = new BlockingIOProcessor(4, 10000);
        reportingDBManager = new ReportingDBManager("db-test.properties", blockingIOProcessor, true);
        assertNotNull(reportingDBManager.getConnection());
    }

    @AfterClass
    public static void close() {
        reportingDBManager.close();
    }

    @Before
    public void cleanAll() throws Exception {
        //clean everything just in case
        reportingDBManager.executeSQL("DELETE FROM reporting_app_stat_minute");
        reportingDBManager.executeSQL("DELETE FROM reporting_app_command_stat_minute");
        reportingDBManager.executeSQL("DELETE FROM reporting_http_command_stat_minute");
        reportingDBManager.executeSQL("DELETE FROM reporting_average_minute");
        reportingDBManager.executeSQL("DELETE FROM reporting_average_hourly");
        reportingDBManager.executeSQL("DELETE FROM reporting_average_daily");
    }

    @Test
    public void testRealTimeStatsInsertWroks() throws Exception {
        String region = "ua";
        long now = System.currentTimeMillis();

        SessionDao sessionDao = new SessionDao();
        UserDao userDao = new UserDao(new ConcurrentHashMap<>(), "test", "127.0.0.1");
        BlockingIOProcessor blockingIOProcessor = new BlockingIOProcessor(6, 1000);

        Stat stat = new Stat(sessionDao, userDao, blockingIOProcessor, new GlobalStats(), new ReportScheduler(1, "http://localhost/", null, null, Collections.emptyMap()), false);
        int i;

        final HttpStat hs = stat.http;
        i = 0;
        hs.isHardwareConnected = i++;
        hs.isAppConnected = i++;
        hs.getPinData = i++;
        hs.updatePinData = i++;
        hs.email = i++;
        hs.notify = i++;
        hs.getProject = i++;
        hs.qr = i++;
        hs.getHistoryPinData = i++;
        hs.total = i;

        final CommandStat cs = stat.commands;
        i = 0;
        cs.response = i++;
        cs.register = i++;
        cs.login = i++;
        cs.loadProfile = i++;
        cs.appSync = i++;
        cs.sharing = i++;
        cs.getToken = i++;
        cs.ping = i++;
        cs.activate = i++;
        cs.deactivate = i++;
        cs.refreshToken = i++;
        cs.getGraphData = i++;
        cs.exportGraphData = i++;
        cs.setWidgetProperty = i++;
        cs.bridge = i++;
        cs.hardware = i++;
        cs.getSharedDash = i++;
        cs.getShareToken = i++;
        cs.refreshShareToken = i++;
        cs.shareLogin = i++;
        cs.createProject = i++;
        cs.updateProject = i++;
        cs.deleteProject = i++;
        cs.hardwareSync = i++;
        cs.internal = i++;
        cs.sms = i++;
        cs.tweet = i++;
        cs.email = i++;
        cs.push = i++;
        cs.addPushToken = i++;
        cs.createWidget = i++;
        cs.updateWidget = i++;
        cs.deleteWidget = i++;
        cs.createDevice = i++;
        cs.updateDevice = i++;
        cs.deleteDevice = i++;
        cs.getDevices = i++;
        cs.createTag = i++;
        cs.updateTag = i++;
        cs.deleteTag = i++;
        cs.getTags = i++;
        cs.addEnergy = i++;
        cs.getEnergy = i++;
        cs.getServer = i++;
        cs.connectRedirect = i++;
        cs.webSockets = i++;
        cs.eventor = i++;
        cs.webhooks = i++;
        cs.appTotal = i++;
        cs.mqttTotal = i;

        reportingDBManager.reportingDBDao.insertStat(region, stat);

        try (Connection connection = reportingDBManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from reporting_app_stat_minute")) {


            while (rs.next()) {
                assertEquals(region, rs.getString("region"));
                assertEquals((now / AverageAggregatorProcessor.MINUTE) * AverageAggregatorProcessor.MINUTE, rs.getTimestamp("ts", UTC).getTime());

                assertEquals(0, rs.getInt("minute_rate"));
                assertEquals(0, rs.getInt("registrations"));
                assertEquals(0, rs.getInt("active"));
                assertEquals(0, rs.getInt("active_week"));
                assertEquals(0, rs.getInt("active_month"));
                assertEquals(0, rs.getInt("connected"));
                assertEquals(0, rs.getInt("online_apps"));
                assertEquals(0, rs.getInt("total_online_apps"));
                assertEquals(0, rs.getInt("online_hards"));
                assertEquals(0, rs.getInt("total_online_hards"));
            }

            connection.commit();
        }

        try (Connection connection = reportingDBManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from reporting_http_command_stat_minute")) {
            i = 0;
            while (rs.next()) {
                assertEquals(region, rs.getString("region"));
                assertEquals((now / AverageAggregatorProcessor.MINUTE) * AverageAggregatorProcessor.MINUTE, rs.getTimestamp("ts", UTC).getTime());

                assertEquals(i++, rs.getInt("is_hardware_connected"));
                assertEquals(i++, rs.getInt("is_app_connected"));
                assertEquals(i++, rs.getInt("get_pin_data"));
                assertEquals(i++, rs.getInt("update_pin"));
                assertEquals(i++, rs.getInt("email"));
                assertEquals(i++, rs.getInt("push"));
                assertEquals(i++, rs.getInt("get_project"));
                assertEquals(i++, rs.getInt("qr"));
                assertEquals(i++, rs.getInt("get_history_pin_data"));
                assertEquals(i++, rs.getInt("total"));
            }

            connection.commit();
        }

        try (Connection connection = reportingDBManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from reporting_app_command_stat_minute")) {
            i = 0;
            while (rs.next()) {
                assertEquals(region, rs.getString("region"));
                assertEquals((now / AverageAggregatorProcessor.MINUTE) * AverageAggregatorProcessor.MINUTE, rs.getTimestamp("ts", UTC).getTime());

                assertEquals(i++, rs.getInt("response"));
                assertEquals(i++, rs.getInt("register"));
                assertEquals(i++, rs.getInt("login"));
                assertEquals(i++, rs.getInt("load_profile"));
                assertEquals(i++, rs.getInt("app_sync"));
                assertEquals(i++, rs.getInt("sharing"));
                assertEquals(i++, rs.getInt("get_token"));
                assertEquals(i++, rs.getInt("ping"));
                assertEquals(i++, rs.getInt("activate"));
                assertEquals(i++, rs.getInt("deactivate"));
                assertEquals(i++, rs.getInt("refresh_token"));
                assertEquals(i++, rs.getInt("get_graph_data"));
                assertEquals(i++, rs.getInt("export_graph_data"));
                assertEquals(i++, rs.getInt("set_widget_property"));
                assertEquals(i++, rs.getInt("bridge"));
                assertEquals(i++, rs.getInt("hardware"));
                assertEquals(i++, rs.getInt("get_share_dash"));
                assertEquals(i++, rs.getInt("get_share_token"));
                assertEquals(i++, rs.getInt("refresh_share_token"));
                assertEquals(i++, rs.getInt("share_login"));
                assertEquals(i++, rs.getInt("create_project"));
                assertEquals(i++, rs.getInt("update_project"));
                assertEquals(i++, rs.getInt("delete_project"));
                assertEquals(i++, rs.getInt("hardware_sync"));
                assertEquals(i++, rs.getInt("internal"));
                assertEquals(i++, rs.getInt("sms"));
                assertEquals(i++, rs.getInt("tweet"));
                assertEquals(i++, rs.getInt("email"));
                assertEquals(i++, rs.getInt("push"));
                assertEquals(i++, rs.getInt("add_push_token"));
                assertEquals(i++, rs.getInt("create_widget"));
                assertEquals(i++, rs.getInt("update_widget"));
                assertEquals(i++, rs.getInt("delete_widget"));
                assertEquals(i++, rs.getInt("create_device"));
                assertEquals(i++, rs.getInt("update_device"));
                assertEquals(i++, rs.getInt("delete_device"));
                assertEquals(i++, rs.getInt("get_devices"));
                assertEquals(i++, rs.getInt("create_tag"));
                assertEquals(i++, rs.getInt("update_tag"));
                assertEquals(i++, rs.getInt("delete_tag"));
                assertEquals(i++, rs.getInt("get_tags"));
                assertEquals(i++, rs.getInt("add_energy"));
                assertEquals(i++, rs.getInt("get_energy"));
                assertEquals(i++, rs.getInt("get_server"));
                assertEquals(i++, rs.getInt("connect_redirect"));
                assertEquals(i++, rs.getInt("web_sockets"));
                assertEquals(i++, rs.getInt("eventor"));
                assertEquals(i++, rs.getInt("webhooks"));
                assertEquals(i++, rs.getInt("appTotal"));
                assertEquals(i, rs.getInt("hardTotal"));
            }

            connection.commit();
        }


    }

    @Test
    public void testManyConnections() throws Exception {
        User user = new User();
        user.email = "test@test.com";
        user.appName = AppNameUtil.BLYNK;
        Map<AggregationKey, AggregationValue> map = new ConcurrentHashMap<>();
        AggregationValue value = new AggregationValue();
        value.update(1);
        long ts = System.currentTimeMillis();
        for (int i = 0; i < 60; i++) {
            map.put(new AggregationKey(user.email, user.appName, i, 0, PinType.ANALOG, (short) i, ts), value);
            reportingDBManager.insertReporting(map, GraphGranularityType.MINUTE);
            reportingDBManager.insertReporting(map, GraphGranularityType.HOURLY);
            reportingDBManager.insertReporting(map, GraphGranularityType.DAILY);

            map.clear();
        }

        while (blockingIOProcessor.messagingExecutor.getActiveCount() > 0) {
            Thread.sleep(100);
        }

    }

    @Test
    public void cleanOutdatedRecords() {
        reportingDBManager.reportingDBDao.cleanOldReportingRecords(Instant.now());
    }

    @Test
    public void testDeleteWorksAsExpected() throws Exception {
        long minute;
        try (Connection connection = reportingDBManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(ReportingDBDao.insertMinute)) {

            minute = (System.currentTimeMillis() / AverageAggregatorProcessor.MINUTE) * AverageAggregatorProcessor.MINUTE;

            for (int i = 0; i < 370; i++) {
                ReportingDBDao.prepareReportingInsert(ps, "test1111@gmail.com", 1, 0, (short) 0, PinType.VIRTUAL, minute, (double) i);
                ps.addBatch();
                minute += AverageAggregatorProcessor.MINUTE;
            }

            ps.executeBatch();
            connection.commit();
        }
    }

    @Test
    public void testInsert1000RecordsAndSelect() throws Exception {
        int a = 0;

        String userName = "test@gmail.com";

        long start = System.currentTimeMillis();
        long minute = (start / AverageAggregatorProcessor.MINUTE) * AverageAggregatorProcessor.MINUTE;
        long startMinute = minute;

        try (Connection connection = reportingDBManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(ReportingDBDao.insertMinute)) {

            for (int i = 0; i < 1000; i++) {
                ReportingDBDao.prepareReportingInsert(ps, userName, 1, 2, (short) 0, PinType.VIRTUAL, minute, (double) i);
                ps.addBatch();
                minute += AverageAggregatorProcessor.MINUTE;
                a++;
            }

            ps.executeBatch();
            connection.commit();
        }

        System.out.println("Finished : " + (System.currentTimeMillis() - start)  + " millis. Executed : " + a);


        try (Connection connection = reportingDBManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from reporting_average_minute order by ts ASC")) {

            int i = 0;
            while (rs.next()) {
                assertEquals(userName, rs.getString("email"));
                assertEquals(1, rs.getInt("project_id"));
                assertEquals(2, rs.getInt("device_id"));
                assertEquals(0, rs.getByte("pin"));
                assertEquals(PinType.VIRTUAL, PinType.values()[rs.getInt("pin_type")]);
                assertEquals(startMinute, rs.getTimestamp("ts", UTC).getTime());
                assertEquals((double) i, rs.getDouble("value"), 0.0001);
                startMinute += AverageAggregatorProcessor.MINUTE;
                i++;
            }
            connection.commit();
        }
    }

    @Test
    public void testSelect() throws Exception {
        long ts = 1455924480000L;
        try (Connection connection = reportingDBManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(ReportingDBDao.selectMinute)) {

            ReportingDBDao.prepareReportingSelect(ps, ts, 2);
            ResultSet rs = ps.executeQuery();


            while(rs.next()) {
                System.out.println(rs.getLong("ts") + " " + rs.getDouble("value"));
            }

            rs.close();
        }
    }
}
