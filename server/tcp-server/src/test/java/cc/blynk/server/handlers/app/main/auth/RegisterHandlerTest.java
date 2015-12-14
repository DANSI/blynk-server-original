package cc.blynk.server.handlers.app.main.auth;

import cc.blynk.common.model.messages.protocol.appllication.RegisterMessage;
import cc.blynk.server.TestBase;
import cc.blynk.server.dao.UserDao;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.08.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class RegisterHandlerTest extends TestBase {

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private UserDao userDao;


    @Test
    public void testRegisterOk() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userDao, "");

        String userName = "test@gmail.com";

        when(userDao.isUserExists(userName)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + " 1"));

        verify(userDao).add(eq(userName), eq("1"));
    }

    @Test
    public void testRegisterOk2() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userDao, null);

        String userName = "test@gmail.com";

        when(userDao.isUserExists(userName)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + " 1"));

        verify(userDao).add(eq(userName), eq("1"));
    }

    @Test
    public void testAllowedUsersSingleUserWork() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userDao, "test@gmail.com");

        String userName = "test@gmail.com";

        when(userDao.isUserExists(userName)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + " 1"));

        verify(userDao).add(eq(userName), eq("1"));
    }

    @Test
    public void testAllowedUsersSingleUserNotWork() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userDao, "test@gmail.com");

        String userName = "test2@gmail.com";

        when(userDao.isUserExists(userName)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + " 1"));

        verify(userDao, times(0)).add(eq(userName), eq("1"));
        verify(ctx).writeAndFlush(eq(produce(1, NOT_ALLOWED)));
    }

    @Test
    public void testAllowedUsersSingleUserWork2() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userDao, "test@gmail.com,test2@gmail.com");

        String userName = "test2@gmail.com";

        when(userDao.isUserExists(userName)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + " 1"));

        verify(userDao).add(eq(userName), eq("1"));
    }

}
