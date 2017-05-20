package cc.blynk.server.db;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.db.model.FlashedToken;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.02.16.
 */
public class FlashedTokensManagerTest {

    private static DBManager dbManager;
    private static BlockingIOProcessor blockingIOProcessor;

    @BeforeClass
    public static void init() throws Exception {
        blockingIOProcessor = new BlockingIOProcessor(4, 5000);
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
        dbManager.executeSQL("DELETE FROM flashed_tokens");
    }

    @Test
    public void test() throws Exception {
        assertNotNull(dbManager.getConnection());
    }

    @Test
    public void testNoToken() throws Exception {
        assertNull(dbManager.selectFlashedToken("123"));
    }

    @Test
    public void testInsertAndSelect() throws Exception {
        FlashedToken[] list = new FlashedToken[1];
        String token = UUID.randomUUID().toString().replace("-", "");
        FlashedToken flashedToken = new FlashedToken("test@blynk.cc", token, "appname", 1, 0);
        list[0] = flashedToken;
        dbManager.insertFlashedTokens(list);


        FlashedToken selected = dbManager.selectFlashedToken(token);

        assertEquals(flashedToken, selected);
    }

    @Test
    public void testInsertToken() throws Exception {
        FlashedToken[] list = new FlashedToken[1];
        String token = UUID.randomUUID().toString().replace("-", "");
        FlashedToken flashedToken = new FlashedToken("test@blynk.cc", token, "appname", 1, 0);
        list[0] = flashedToken;
        dbManager.insertFlashedTokens(list);

        try (Connection connection = dbManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from flashed_tokens")) {

            while (rs.next()) {
                assertEquals(flashedToken.token, rs.getString("token"));
                assertEquals(flashedToken.appId, rs.getString("app_name"));
                assertEquals(flashedToken.deviceId, rs.getInt("device_id"));
                assertFalse(rs.getBoolean("is_activated"));
                assertNull(rs.getDate("ts"));
            }

            connection.commit();
        }
    }

    @Test
    public void testInsertAndActivate() throws Exception {
        FlashedToken[] list = new FlashedToken[1];
        String token = UUID.randomUUID().toString().replace("-", "");
        FlashedToken flashedToken = new FlashedToken("test@blynk.cc", token, "appname", 1, 0);
        list[0] = flashedToken;
        dbManager.insertFlashedTokens(list);
        dbManager.activateFlashedToken(flashedToken.token);

        try (Connection connection = dbManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from flashed_tokens")) {

            while (rs.next()) {
                assertEquals(flashedToken.token, rs.getString("token"));
                assertEquals(flashedToken.appId, rs.getString("app_name"));
                assertEquals(flashedToken.deviceId, rs.getInt("device_id"));
                assertTrue(rs.getBoolean("is_activated"));
                assertNotNull(rs.getDate("ts"));
            }

            connection.commit();
        }
    }

}

