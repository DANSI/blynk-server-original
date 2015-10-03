package cc.blynk.server.handlers.hardware;

import cc.blynk.common.enums.Command;
import cc.blynk.common.model.messages.MessageFactory;
import cc.blynk.common.model.messages.protocol.hardware.MailMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.TestBase;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.handlers.hardware.logic.MailLogic;
import cc.blynk.server.model.Profile;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Mail;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.04.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class MailHandlerTest extends TestBase {

    @Mock
    private NotificationsProcessor notificationsProcessor;

    private MailLogic mailHandler = new MailLogic(notificationsProcessor, 1);

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
    private Channel channel;

    @Test(expected = NotAllowedException.class)
	public void testNoEmailWidget() throws InterruptedException {
		MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "body");

        user.profile = profile;
        when(profile.getActiveDashboardWidgetByType(Mail.class)).thenReturn(null);

        mailHandler.messageReceived(ctx, user, mailMessage);
    }

    @Test(expected = IllegalCommandException.class)
	public void testNoToBody() throws InterruptedException {
		MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "".replaceAll(" ", "\0"));

        user.profile = profile;
        cc.blynk.server.model.widgets.others.Mail mail = new cc.blynk.server.model.widgets.others.Mail();
        when(profile.getActiveDashboardWidgetByType(cc.blynk.server.model.widgets.others.Mail.class)).thenReturn(mail);

        mailHandler.messageReceived(ctx, user, mailMessage);
    }

    @Test(expected = IllegalCommandException.class)
	public void testNoBody() throws InterruptedException {
		MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "body".replaceAll(" ", "\0"));

        user.profile = profile;
        when(profile.getActiveDashboardWidgetByType(cc.blynk.server.model.widgets.others.Mail.class)).thenReturn(new cc.blynk.server.model.widgets.others.Mail());

        mailHandler.messageReceived(ctx, user, mailMessage);
    }

}
