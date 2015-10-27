package cc.blynk.server.handlers.app;

import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.TestBase;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.app.logic.HardwareAppLogic;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.server.handlers.hardware.logic.HardwareLogic;
import cc.blynk.server.model.Profile;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.workers.notifications.BlockingIOProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * The Blynk Project.
 * Created by Andrew Zakordonets.
 * Created on 29.04.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HardwareAppHandlerTest extends TestBase {

    @Mock
    private BlockingIOProcessor blockingIOProcessor;

    @InjectMocks
    private HardwareLogic hardwareHandler;

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private UserDao userDao;

    @InjectMocks
    private SessionDao sessionDao;

    @Mock
    private ServerProperties serverProperties;

    @Mock
    private User user;

    @Mock
    private Profile profile;

    @Mock
    private Channel channel;

    @Test
    public void testActiveDashboardIdNull() {
        HardwareMessage message = new HardwareMessage(1, "test");
        when(ctx.channel()).thenReturn(channel);
        user.profile = profile;
        profile.activeDashId = null;
        SessionDao sessionDao = spy(new SessionDao());
        HardwareAppLogic hardwareHandler = spy(new HardwareAppLogic(sessionDao));
        HardwareStateHolder handlerState = new HardwareStateHolder(1, user, "x");
        hardwareHandler.messageReceived(ctx, handlerState, message);
    }

}
