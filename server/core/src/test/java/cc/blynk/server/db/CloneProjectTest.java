package cc.blynk.server.db;

import cc.blynk.server.core.BlockingIOProcessor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.02.16.
 */
public class CloneProjectTest {

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
        dbManager.executeSQL("DELETE FROM cloned_projects");
    }

    @Test
    public void testNoToken() throws Exception {
        assertNull(dbManager.cloneProjectDBDao.selectClonedProjectByToken("123"));
    }

    @Test
    public void testInsertAndSelect() throws Exception {
        assertTrue(dbManager.insertClonedProject("token", "json"));
        assertEquals("json", dbManager.selectClonedProject("token"));
    }

    @Test
    public void testInsertAndSelectWrong() throws Exception {
        assertTrue(dbManager.insertClonedProject("token", "json"));
        assertNull(dbManager.selectClonedProject("token2"));
    }

}

