package cc.blynk.server.handlers.hardware;

import cc.blynk.common.enums.Command;
import cc.blynk.common.model.messages.MessageFactory;
import cc.blynk.common.model.messages.protocol.hardware.MailMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.exceptions.IllegalCommandException;
import cc.blynk.server.core.exceptions.NotAllowedException;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.notifications.Mail;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.server.handlers.hardware.logic.MailLogic;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.04.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class MailHandlerTest {

    @Mock
    private BlockingIOProcessor blockingIOProcessor;

    private MailLogic mailHandler = new MailLogic(blockingIOProcessor, 1);

	@Mock
	private ChannelHandlerContext ctx;

	@Mock
	private UserDao userDao;

	@Mock
	private SessionDao sessionDao;

	@Mock
	private ServerProperties serverProperties;

    @Mock
    private User user;

    @Mock
    private Profile profile;

    @Mock
    private DashBoard dashBoard;

    @Mock
    private Channel channel;

    @Test(expected = NotAllowedException.class)
	public void testNoEmailWidget() throws InterruptedException {
		MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "body");

        user.profile = profile;
        when(profile.getDashById(1, 1)).thenReturn(dashBoard);
        when(dashBoard.getWidgetByType(Mail.class)).thenReturn(null);

        HardwareStateHolder state = new HardwareStateHolder(1, user, "x");
        mailHandler.messageReceived(ctx, state, mailMessage);
    }

    @Test(expected = IllegalCommandException.class)
	public void testNoToBody() throws InterruptedException {
		MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "".replaceAll(" ", "\0"));

        user.profile = profile;
        when(profile.getDashById(1, 1)).thenReturn(dashBoard);
        Mail mail = new Mail();
        when(dashBoard.getWidgetByType(Mail.class)).thenReturn(mail);
        dashBoard.isActive = true;

        HardwareStateHolder state = new HardwareStateHolder(1, user, "x");
        mailHandler.messageReceived(ctx, state, mailMessage);
    }

    @Test(expected = IllegalCommandException.class)
	public void testNoBody() throws InterruptedException {
		MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "body".replaceAll(" ", "\0"));

        user.profile = profile;
        when(profile.getDashById(1, 1)).thenReturn(dashBoard);
        when(dashBoard.getWidgetByType(Mail.class)).thenReturn(new Mail());
        dashBoard.isActive = true;

        HardwareStateHolder state = new HardwareStateHolder(1, user, "x");
        mailHandler.messageReceived(ctx, state, mailMessage);
    }

}
