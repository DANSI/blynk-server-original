package cc.blynk.server.handlers.app.auth;

import cc.blynk.common.model.messages.protocol.appllication.RegisterMessage;
import cc.blynk.server.TestBase;
import cc.blynk.server.dao.UserRegistry;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static cc.blynk.common.enums.Response.NOT_ALLOWED;
import static cc.blynk.common.model.messages.MessageFactory.produce;
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
    private UserRegistry userRegistry;


    @Test
    public void testRegisterOk() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userRegistry, "");

        String userName = "test@gmail.com";

        when(userRegistry.isUserExists(userName)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + " 1"));

        verify(userRegistry).createNewUser(eq(userName), eq("1"));
    }

    @Test
    public void testRegisterOk2() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userRegistry, null);

        String userName = "test@gmail.com";

        when(userRegistry.isUserExists(userName)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + " 1"));

        verify(userRegistry).createNewUser(eq(userName), eq("1"));
    }

    @Test
    public void testAllowedUsersSingleUserWork() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userRegistry, "test@gmail.com");

        String userName = "test@gmail.com";

        when(userRegistry.isUserExists(userName)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + " 1"));

        verify(userRegistry).createNewUser(eq(userName), eq("1"));
    }

    @Test
    public void testAllowedUsersSingleUserNotWork() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userRegistry, "test@gmail.com");

        String userName = "test2@gmail.com";

        when(userRegistry.isUserExists(userName)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + " 1"));

        verify(userRegistry, times(0)).createNewUser(eq(userName), eq("1"));
        verify(ctx).writeAndFlush(eq(produce(1, NOT_ALLOWED)));
    }

    @Test
    public void testAllowedUsersSingleUserWork2() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userRegistry, "test@gmail.com,test2@gmail.com");

        String userName = "test2@gmail.com";

        when(userRegistry.isUserExists(userName)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + " 1"));

        verify(userRegistry).createNewUser(eq(userName), eq("1"));
    }

}
