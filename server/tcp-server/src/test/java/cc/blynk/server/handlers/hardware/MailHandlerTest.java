package cc.blynk.server.handlers.hardware;

import cc.blynk.common.enums.Command;
import cc.blynk.common.model.messages.MessageFactory;
import cc.blynk.common.model.messages.protocol.hardware.MailMessage;
import cc.blynk.server.TestBase;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.model.UserProfile;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Email;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.04.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class MailHandlerTest extends TestBase {

    @Mock
    private NotificationsProcessor notificationsProcessor;

    @InjectMocks
    private MailHandler mailHandler;

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private User user;

    @Mock
    private UserProfile userProfile;

    @Mock
    private Channel channel;

    @Test(expected = NotAllowedException.class)
    public void testNoEmailWidget() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "body");

        when(user.getUserProfile()).thenReturn(userProfile);
        when(userProfile.getActiveDashboardEmailWidget()).thenReturn(null);

        mailHandler.messageReceived(ctx, user, mailMessage);
    }

    @Test(expected = IllegalCommandException.class)
    public void testNoToBody() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "".replaceAll(" ", "\0"));

        when(user.getUserProfile()).thenReturn(userProfile);
        Email email = new Email();
        when(userProfile.getActiveDashboardEmailWidget()).thenReturn(email);

        mailHandler.messageReceived(ctx, user, mailMessage);
    }

    @Test(expected = IllegalCommandException.class)
    public void testNoBody() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "body".replaceAll(" ", "\0"));

        when(user.getUserProfile()).thenReturn(userProfile);
        when(userProfile.getActiveDashboardEmailWidget()).thenReturn(new Email());

        mailHandler.messageReceived(ctx, user, mailMessage);
    }

    @Test
    public void sendEmptyBodyMailYoUseDefaults() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "");

        when(user.getUserProfile()).thenReturn(userProfile);
        Email email = new Email("me@example.com", "Yo", "MyBody");
        when(userProfile.getActiveDashboardEmailWidget()).thenReturn(email);

        when(ctx.channel()).thenReturn(channel);

        mailHandler.messageReceived(ctx, user, mailMessage);
        verify(notificationsProcessor).mail(eq("me@example.com"), eq("Yo"), eq("MyBody"), eq(1));
        verify(ctx).channel();
    }

    @Test
    public void sendEmptyBodyMailYoUseDefaultsExceptBody() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "body".replaceAll(" ", "\0"));

        when(user.getUserProfile()).thenReturn(userProfile);
        Email email = new Email("me@example.com", "Yo", "MyBody");
        when(userProfile.getActiveDashboardEmailWidget()).thenReturn(email);

        when(ctx.channel()).thenReturn(channel);

        mailHandler.messageReceived(ctx, user, mailMessage);
        verify(notificationsProcessor).mail(eq("me@example.com"), eq("Yo"), eq("body"), eq(1));
        verify(ctx).channel();
    }

    @Test
    public void sendEmptyBodyMailYoUseDefaultsExceptBodyAndSubj() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "subj body".replaceAll(" ", "\0"));

        when(user.getUserProfile()).thenReturn(userProfile);
        Email email = new Email("me@example.com", "Yo", "MyBody");
        when(userProfile.getActiveDashboardEmailWidget()).thenReturn(email);

        when(ctx.channel()).thenReturn(channel);

        mailHandler.messageReceived(ctx, user, mailMessage);
        verify(notificationsProcessor).mail(eq("me@example.com"), eq("subj"), eq("body"), eq(1));
        verify(ctx).channel();
    }

    @Test
    public void sendEmptyBodyMailYoNoDefaults() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "pupkin@example.com subj body".replaceAll(" ", "\0"));

        when(user.getUserProfile()).thenReturn(userProfile);
        Email email = new Email("me@example.com", "Yo", "MyBody");
        when(userProfile.getActiveDashboardEmailWidget()).thenReturn(email);

        when(ctx.channel()).thenReturn(channel);

        mailHandler.messageReceived(ctx, user, mailMessage);
        verify(notificationsProcessor).mail(eq("pupkin@example.com"), eq("subj"), eq("body"), eq(1));
        verify(ctx).channel();
    }

}
