package cc.blynk.server.admin.http.logic.admin;

import cc.blynk.core.http.Response;
import cc.blynk.server.admin.http.logic.UsersLogic;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.db.DBManager;
import cc.blynk.utils.AppNameUtil;
import io.netty.channel.EventLoop;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UsersLogicTest {

    @Mock
    private UserDao userDao;
    @Spy
    private SessionDao sessionDao;
    @Mock
    private DBManager dbManager;
    @Mock
    private ReportingDiskDao reportingDao;

    private User user;

    @Mock
    private Session session;

    private UsersLogic usersLogic;
    private static final String TEST_USER = "test_user";
    private static Path userFile;
    private static Path deletedUserFile;
    private static final String DELETED_DATA_DIR_NAME = "deleted";

    @Before
    public void setUp() throws Exception {
        user = new User(TEST_USER, "123", AppNameUtil.BLYNK, "local", "127.0.0.1", false, false);
        when(userDao.delete(any())).thenReturn(user);
        sessionDao.getOrCreateSessionByUser(new UserKey(user), mock(EventLoop.class));
        FileManager fileManager = new FileManager(null, null);
        usersLogic = new UsersLogic(userDao, sessionDao, dbManager, fileManager, null, reportingDao, "admin");

        userFile = Paths.get(System.getProperty("java.io.tmpdir"), "blynk", TEST_USER + ".Blynk.user");
        deletedUserFile = Paths.get(System.getProperty("java.io.tmpdir"), "blynk", DELETED_DATA_DIR_NAME, TEST_USER + ".Blynk.user");
        Files.deleteIfExists(userFile);
        Files.deleteIfExists(deletedUserFile);

        Files.createFile(userFile);
    }

    @Test
    public void deleteUserByName() throws Exception {
        Response response = usersLogic.deleteUserByName(TEST_USER + "-" + AppNameUtil.BLYNK);

        assertEquals(OK, response.status());
        assertFalse(Files.exists(userFile));
        assertTrue(Files.exists(deletedUserFile));
    }

    @Test
    public void deleteFakeUserByName() throws Exception {
        Response response = usersLogic.deleteUserByName("fake user" + "-" + AppNameUtil.BLYNK);

        assertEquals(NOT_FOUND, response.status());
    }

}