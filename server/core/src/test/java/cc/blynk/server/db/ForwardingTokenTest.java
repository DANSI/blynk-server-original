package cc.blynk.server.db;

import cc.blynk.server.core.BlockingIOProcessor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.02.16.
 */
public class ForwardingTokenTest {

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
        dbManager.executeSQL("DELETE FROM forwarding_tokens");
    }

    @Test
    public void testNoToken() throws Exception {
        assertNull(dbManager.forwardingTokenDBDao.selectHostByToken("123"));
    }

    @Test
    public void testInsertAndSelect() throws Exception {
        assertTrue(dbManager.forwardingTokenDBDao.insertTokenHost("token", "host", "email", 0, 0));
        assertEquals("host", dbManager.forwardingTokenDBDao.selectHostByToken("token"));
    }

    @Test
    public void testInsertAndSelectWrong() throws Exception {
        assertTrue(dbManager.forwardingTokenDBDao.insertTokenHost("token", "host", "email", 0, 0));
        assertNull(dbManager.forwardingTokenDBDao.selectHostByToken("token2"));
    }

    @Test
    public void deleteToken() throws Exception {
        assertTrue(dbManager.forwardingTokenDBDao.insertTokenHost("token", "host", "email", 0, 0));
        assertTrue(dbManager.forwardingTokenDBDao.deleteToken("token"));
        assertNull(dbManager.forwardingTokenDBDao.selectHostByToken("token"));
    }

    @Test
    public void invalidToken() throws Exception {
        assertNull(dbManager.forwardingTokenDBDao.selectHostByToken("\0"));
    }

    @Test
    public void deleteTokens() throws Exception {
        assertTrue(dbManager.forwardingTokenDBDao.insertTokenHost("token1", "host1", "email", 0, 0));
        assertTrue(dbManager.forwardingTokenDBDao.insertTokenHost("token2", "host2", "email", 0, 0));
        assertTrue(dbManager.forwardingTokenDBDao.insertTokenHost("token3", "host3", "email", 0, 0));
        assertTrue(dbManager.forwardingTokenDBDao.deleteToken("token1", "token2"));
        assertNull(dbManager.forwardingTokenDBDao.selectHostByToken("token1"));
        assertNull(dbManager.forwardingTokenDBDao.selectHostByToken("token2"));
        assertEquals("host3", dbManager.forwardingTokenDBDao.selectHostByToken("token3"));
    }
}

