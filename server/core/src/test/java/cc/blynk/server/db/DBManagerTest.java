package cc.blynk.server.db;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.reporting.average.AverageAggregatorProcessor;
import cc.blynk.server.db.dao.ReportingDBDao;
import cc.blynk.server.db.model.Purchase;
import cc.blynk.server.db.model.Redeem;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.DateTimeUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.02.16.
 */
public class DBManagerTest {

    private static DBManager dbManager;
    private static BlockingIOProcessor blockingIOProcessor;
    private static final Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    @BeforeClass
    public static void init() throws Exception {
        blockingIOProcessor = new BlockingIOProcessor(4, 10000);
        dbManager = new DBManager("db-test.properties", blockingIOProcessor, true);
        assertNotNull(dbManager.getConnection());
    }

    @AfterClass
    public static void close() {
        dbManager.close();
    }

    @Before
    public void cleanAll() throws Exception {
        //clean everything just in case
        dbManager.executeSQL("DELETE FROM users");
        dbManager.executeSQL("DELETE FROM purchase");
        dbManager.executeSQL("DELETE FROM redeem");
    }

    @Test
    public void test() throws Exception {
        assertNotNull(dbManager.getConnection());
    }

    @Test
    public void testDbVersion() throws Exception {
        int dbVersion = dbManager.userDBDao.getDBVersion();
        assertTrue(dbVersion >= 90500);
    }

    @Test
    @Ignore("not used right now in read code")
    public void testCopy100RecordsIntoFile() throws Exception {
        System.out.println("Starting");

        int a = 0;

        long start = System.currentTimeMillis();
        try (Connection connection = dbManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(ReportingDBDao.insertMinute)) {

            String userName = "test@gmail.com";
            long minute = (System.currentTimeMillis() / AverageAggregatorProcessor.MINUTE) * AverageAggregatorProcessor.MINUTE;

            for (int i = 0; i < 100; i++) {
                ReportingDBDao.prepareReportingInsert(ps, userName, 1, 0, (short) 0, PinType.VIRTUAL, minute, (double) i);
                ps.addBatch();
                minute += AverageAggregatorProcessor.MINUTE;
                a++;
            }

            ps.executeBatch();
            connection.commit();
        }

        System.out.println("Finished : " + (System.currentTimeMillis() - start)  + " millis. Executed : " + a);


        try (Connection connection = dbManager.getConnection();
             Writer gzipWriter = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File("/home/doom369/output.csv.gz"))), "UTF-8")) {

            CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));


            String selectQuery = "select pintype || pin, ts, value from reporting_average_minute where project_id = 1 and email = 'test@gmail.com'";
            long res = copyManager.copyOut("COPY (" + selectQuery + " ) TO STDOUT WITH (FORMAT CSV)", gzipWriter);
            System.out.println(res);
        }


    }

    @Test
    public void testUpsertForDifferentApps() throws Exception {
        ArrayList<User> users = new ArrayList<>();
        users.add(new User("test1@gmail.com", "pass", "testapp2", "local", "127.0.0.1", false, false));
        users.add(new User("test1@gmail.com", "pass", "testapp1", "local", "127.0.0.1", false, false));
        dbManager.userDBDao.save(users);
        ConcurrentMap<UserKey, User> dbUsers = dbManager.userDBDao.getAllUsers("local");
        assertEquals(2, dbUsers.size());
    }

    @Test
    public void testUpsertAndSelect() throws Exception {
        ArrayList<User> users = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            users.add(new User("test" + i + "@gmail.com", "pass", AppNameUtil.BLYNK, "local", "127.0.0.1", false, false));
        }
        //dbManager.saveUsers(users);
        dbManager.userDBDao.save(users);

        ConcurrentMap<UserKey, User> dbUsers = dbManager.userDBDao.getAllUsers("local");
        System.out.println("Records : " + dbUsers.size());
    }

    @Test
    public void testUpsertUser() throws Exception {
        ArrayList<User> users = new ArrayList<>();
        User user = new User("test@gmail.com", "pass", AppNameUtil.BLYNK, "local", "127.0.0.1", false, false);
        user.name = "123";
        user.lastModifiedTs = 0;
        user.lastLoggedAt = 1;
        user.lastLoggedIP = "127.0.0.1";
        users.add(user);
        user = new User("test@gmail.com", "pass", AppNameUtil.BLYNK, "local", "127.0.0.1", false, false);
        user.lastModifiedTs = 0;
        user.lastLoggedAt = 1;
        user.lastLoggedIP = "127.0.0.1";
        user.name = "123";
        users.add(user);
        user = new User("test2@gmail.com", "pass", AppNameUtil.BLYNK, "local", "127.0.0.1", false, false);
        user.lastModifiedTs = 0;
        user.lastLoggedAt = 1;
        user.lastLoggedIP = "127.0.0.1";
        user.name = "123";
        users.add(user);

        dbManager.userDBDao.save(users);

        try (Connection connection = dbManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from users where email = 'test@gmail.com'")) {
            while (rs.next()) {
                assertEquals("test@gmail.com", rs.getString("email"));
                assertEquals(AppNameUtil.BLYNK, rs.getString("appName"));
                assertEquals("local", rs.getString("region"));
                assertEquals("123", rs.getString("name"));
                assertEquals("pass", rs.getString("pass"));
                assertEquals(0, rs.getTimestamp("last_modified", DateTimeUtils.UTC_CALENDAR).getTime());
                assertEquals(1, rs.getTimestamp("last_logged", DateTimeUtils.UTC_CALENDAR).getTime());
                assertEquals("127.0.0.1", rs.getString("last_logged_ip"));
                assertFalse(rs.getBoolean("is_facebook_user"));
                assertFalse(rs.getBoolean("is_super_admin"));
                assertEquals(2000, rs.getInt("energy"));

                assertEquals("{}", rs.getString("json"));
            }
            connection.commit();
        }
    }

    @Test
    public void testUpsertUserFieldUpdated() throws Exception {
        ArrayList<User> users = new ArrayList<>();
        User user = new User("test@gmail.com", "pass", AppNameUtil.BLYNK, "local", "127.0.0.1", false, false);
        user.lastModifiedTs = 0;
        user.lastLoggedAt = 1;
        user.lastLoggedIP = "127.0.0.1";
        users.add(user);

        dbManager.userDBDao.save(users);

        users = new ArrayList<>();
        user = new User("test@gmail.com", "pass2", AppNameUtil.BLYNK, "local2", "127.0.0.1", true, true);
        user.name = "1234";
        user.lastModifiedTs = 1;
        user.lastLoggedAt = 2;
        user.lastLoggedIP = "127.0.0.2";
        user.energy = 1000;
        user.profile = new Profile();
        DashBoard dash = new DashBoard();
        dash.id = 1;
        dash.name = "123";
        user.profile.dashBoards = new DashBoard[]{dash};

        users.add(user);

        dbManager.userDBDao.save(users);

        ConcurrentMap<UserKey, User>  persistent = dbManager.userDBDao.getAllUsers("local2");

        user = persistent.get(new UserKey("test@gmail.com", AppNameUtil.BLYNK));

        assertEquals("test@gmail.com", user.email);
        assertEquals(AppNameUtil.BLYNK, user.appName);
        assertEquals("local2", user.region);
        assertEquals("pass2", user.pass);
        assertEquals("1234", user.name);
        assertEquals("127.0.0.1", user.ip);
        assertEquals(1, user.lastModifiedTs);
        assertEquals(2, user.lastLoggedAt);
        assertEquals("127.0.0.2", user.lastLoggedIP);
        assertTrue(user.isFacebookUser);
        assertTrue(user.isSuperAdmin);
        assertEquals(1000, user.energy);

        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"123\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}", user.profile.toString());
    }

    @Test
    public void testInsertAndGetUser() throws Exception {
        ArrayList<User> users = new ArrayList<>();
        User user = new User("test@gmail.com", "pass", AppNameUtil.BLYNK, "local", "127.0.0.1", true, true);
        user.lastModifiedTs = 0;
        user.lastLoggedAt = 1;
        user.lastLoggedIP = "127.0.0.1";
        user.profile = new Profile();
        DashBoard dash = new DashBoard();
        dash.id = 1;
        dash.name = "123";
        user.profile.dashBoards = new DashBoard[]{dash};
        users.add(user);

        dbManager.userDBDao.save(users);

        ConcurrentMap<UserKey, User> dbUsers = dbManager.userDBDao.getAllUsers("local");

        assertNotNull(dbUsers);
        assertEquals(1, dbUsers.size());
        User dbUser = dbUsers.get(new UserKey(user.email, user.appName));

        assertEquals("test@gmail.com", dbUser.email);
        assertEquals(AppNameUtil.BLYNK, dbUser.appName);
        assertEquals("local", dbUser.region);
        assertEquals("pass", dbUser.pass);
        assertEquals(0, dbUser.lastModifiedTs);
        assertEquals(1, dbUser.lastLoggedAt);
        assertEquals("127.0.0.1", dbUser.lastLoggedIP);
        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"123\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}", dbUser.profile.toString());
        assertTrue(dbUser.isFacebookUser);
        assertTrue(dbUser.isSuperAdmin);
        assertEquals(2000, dbUser.energy);

        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"123\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}", dbUser.profile.toString());
    }

    @Test
    public void testInsertGetDeleteUser() throws Exception {
        ArrayList<User> users = new ArrayList<>();
        User user = new User("test@gmail.com", "pass", AppNameUtil.BLYNK, "local", "127.0.0.1", true, true);
        user.lastModifiedTs = 0;
        user.lastLoggedAt = 1;
        user.lastLoggedIP = "127.0.0.1";
        user.profile = new Profile();
        DashBoard dash = new DashBoard();
        dash.id = 1;
        dash.name = "123";
        user.profile.dashBoards = new DashBoard[]{dash};
        users.add(user);

        dbManager.userDBDao.save(users);

        Map<UserKey, User> dbUsers = dbManager.userDBDao.getAllUsers("local");

        assertNotNull(dbUsers);
        assertEquals(1, dbUsers.size());
        User dbUser = dbUsers.get(new UserKey(user.email, user.appName));

        assertEquals("test@gmail.com", dbUser.email);
        assertEquals(AppNameUtil.BLYNK, dbUser.appName);
        assertEquals("local", dbUser.region);
        assertEquals("pass", dbUser.pass);
        assertEquals(0, dbUser.lastModifiedTs);
        assertEquals(1, dbUser.lastLoggedAt);
        assertEquals("127.0.0.1", dbUser.lastLoggedIP);
        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"123\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}", dbUser.profile.toString());
        assertTrue(dbUser.isFacebookUser);
        assertTrue(dbUser.isSuperAdmin);
        assertEquals(2000, dbUser.energy);

        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"123\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}", dbUser.profile.toString());

        assertTrue(dbManager.userDBDao.deleteUser(new UserKey(user.email, user.appName)));
        dbUsers = dbManager.userDBDao.getAllUsers("local");
        assertNotNull(dbUsers);
        assertEquals(0, dbUsers.size());
    }

    @Test
    public void testRedeem() throws Exception {
        assertNull(dbManager.selectRedeemByToken("123"));
        String token = UUID.randomUUID().toString().replace("-", "");
        dbManager.executeSQL("insert into redeem (token) values('" + token + "')");
        assertNotNull(dbManager.selectRedeemByToken(token));
        assertNull(dbManager.selectRedeemByToken("123"));
    }

    @Test
    public void testPurchase() throws Exception {
        dbManager.insertPurchase(new Purchase("test@gmail.com", 1000, 1.00D, "123456"));


        try (Connection connection = dbManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from purchase")) {

            while (rs.next()) {
                assertEquals("test@gmail.com", rs.getString("email"));
                assertEquals(1000, rs.getInt("reward"));
                assertEquals("123456", rs.getString("transactionId"));
                assertEquals(0.99D, rs.getDouble("price"), 0.1D);
                assertNotNull(rs.getDate("ts"));
            }

            connection.commit();
        }
    }

    @Test
    public void testOptimisticLockingRedeem() throws Exception {
        String token = UUID.randomUUID().toString().replace("-", "");
        dbManager.executeSQL("insert into redeem (token) values('" + token + "')");

        Redeem redeem = dbManager.selectRedeemByToken(token);
        assertNotNull(redeem);
        assertEquals(redeem.token, token);
        assertFalse(redeem.isRedeemed);
        assertEquals(1, redeem.version);
        assertNull(redeem.ts);

        assertTrue(dbManager.updateRedeem("user@user.com", token));
        assertFalse(dbManager.updateRedeem("user@user.com", token));

        redeem = dbManager.selectRedeemByToken(token);
        assertNotNull(redeem);
        assertEquals(redeem.token, token);
        assertTrue(redeem.isRedeemed);
        assertEquals(2, redeem.version);
        assertEquals("user@user.com", redeem.email);
        assertNotNull(redeem.ts);
    }

    @Test
    public void getUserIpNotExists() {
        String userIp = dbManager.userDBDao.getUserServerIp("test@gmail.com", AppNameUtil.BLYNK);
        assertNull(userIp);
    }

    @Test
    public void getUserIp() {
        ArrayList<User> users = new ArrayList<>();
        User user = new User("test@gmail.com", "pass", AppNameUtil.BLYNK, "local", "127.0.0.1", false, false);
        user.lastModifiedTs = 0;
        user.lastLoggedAt = 1;
        user.lastLoggedIP = "127.0.0.1";
        users.add(user);

        dbManager.userDBDao.save(users);

        String userIp = dbManager.userDBDao.getUserServerIp("test@gmail.com", AppNameUtil.BLYNK);
        assertEquals("127.0.0.1", userIp);
    }
}
