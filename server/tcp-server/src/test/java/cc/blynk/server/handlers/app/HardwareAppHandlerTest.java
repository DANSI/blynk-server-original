package cc.blynk.server.handlers.app;

import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.TestBase;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.app.logic.HardwareAppLogic;
import cc.blynk.server.handlers.hardware.logic.HardwareLogic;
import cc.blynk.server.model.Profile;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.storage.StorageDao;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
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
    private NotificationsProcessor notificationsProcessor;

    @InjectMocks
    private HardwareLogic hardwareHandler;

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private UserRegistry userRegistry;

    @InjectMocks
    private SessionsHolder sessionsHolder;

    @Mock
    private ServerProperties serverProperties;

    @Mock
    private User user;

    @Mock
    private Profile profile;

    @Mock
    private Channel channel;

    @Mock
    private Attribute<Boolean> attr;

    @Mock
    private Session session;

    @Mock
    private StorageDao storageDao;

    @Test
    public void testActiveDashboardIdNull() {
        HardwareMessage message = new HardwareMessage(1, "test");
        when(ctx.channel()).thenReturn(channel);
        when(user.getProfile()).thenReturn(profile);
        profile.activeDashId = null;
        SessionsHolder sessionsHolder = spy(new SessionsHolder());
        HardwareAppLogic hardwareHandler = spy(new HardwareAppLogic(sessionsHolder));
        hardwareHandler.messageReceived(ctx, user, message);
    }

}
