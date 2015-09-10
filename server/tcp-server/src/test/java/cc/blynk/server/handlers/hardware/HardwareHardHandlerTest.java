package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.TestBase;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.DeviceNotInNetworkException;
import cc.blynk.server.handlers.hardware.logic.HardwareLogic;
import cc.blynk.server.model.Profile;
import cc.blynk.server.model.auth.ChannelState;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.storage.StorageDao;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import org.junit.Assert;
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
public class HardwareHardHandlerTest extends TestBase {

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
    private Attribute<Integer> attr;

    @Mock
    private Session session;

    @Mock
    private StorageDao storageDao;

    @Test
    public void testNoDeviceAndPinModeMessage() {
        HardwareMessage message = new HardwareMessage(1, "p test");
        when(ctx.channel()).thenReturn(channel);
        when(channel.attr(ChannelState.DASH_ID)).thenReturn(attr);
        Profile profile = spy(new Profile());
        when(attr.get()).thenReturn(1);
        when(user.getProfile()).thenReturn(profile);
        profile.activeDashId = 1;
        SessionsHolder sessionsHolder = spy(new SessionsHolder());
        final Session session = new Session();
        sessionsHolder.userSession.put(user, session);
        HardwareLogic hardwareHandler = spy(new HardwareLogic(sessionsHolder, storageDao));
        try{
            hardwareHandler.messageReceived(ctx, user, message);
        }catch (DeviceNotInNetworkException e){
            Assert.assertEquals(message, profile.pinModeMessage);
        }
    }
}
