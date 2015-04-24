package cc.blynk.server.handlers.hardware;

import cc.blynk.common.enums.Command;
import cc.blynk.common.model.messages.MessageFactory;
import cc.blynk.common.model.messages.protocol.hardware.MailMessage;
import cc.blynk.server.TestBase;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.model.Profile;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Mail;
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
    private Profile profile;

    @Mock
    private Channel channel;

    @Test(expected = NotAllowedException.class)
    public void testNoEmailWidget() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "body");

        when(user.getProfile()).thenReturn(profile);
        when(profile.getActiveDashboardEmailWidget()).thenReturn(null);

        mailHandler.messageReceived(ctx, user, mailMessage);
    }

    @Test(expected = IllegalCommandException.class)
    public void testNoToBody() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "".replaceAll(" ", "\0"));

        when(user.getProfile()).thenReturn(profile);
        Mail mail = new Mail();
        when(profile.getActiveDashboardEmailWidget()).thenReturn(mail);

        mailHandler.messageReceived(ctx, user, mailMessage);
    }

    @Test(expected = IllegalCommandException.class)
    public void testNoBody() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "body".replaceAll(" ", "\0"));

        when(user.getProfile()).thenReturn(profile);
        when(profile.getActiveDashboardEmailWidget()).thenReturn(new Mail());

        mailHandler.messageReceived(ctx, user, mailMessage);
    }

    @Test
    public void sendEmptyBodyMailYoUseDefaults() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "");

        when(user.getProfile()).thenReturn(profile);
        Mail mail = new Mail("me@example.com", "Yo", "MyBody");
        when(profile.getActiveDashboardEmailWidget()).thenReturn(mail);

        when(ctx.channel()).thenReturn(channel);

        mailHandler.messageReceived(ctx, user, mailMessage);
        verify(notificationsProcessor).mail(eq("me@example.com"), eq("Yo"), eq("MyBody"), eq(1));
        verify(ctx).channel();
    }

    @Test
    public void sendEmptyBodyMailYoUseDefaultsExceptBody() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "body".replaceAll(" ", "\0"));

        when(user.getProfile()).thenReturn(profile);
        Mail mail = new Mail("me@example.com", "Yo", "MyBody");
        when(profile.getActiveDashboardEmailWidget()).thenReturn(mail);

        when(ctx.channel()).thenReturn(channel);

        mailHandler.messageReceived(ctx, user, mailMessage);
        verify(notificationsProcessor).mail(eq("me@example.com"), eq("Yo"), eq("body"), eq(1));
        verify(ctx).channel();
    }

    @Test
    public void sendEmptyBodyMailYoUseDefaultsExceptBodyAndSubj() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "subj body".replaceAll(" ", "\0"));

        when(user.getProfile()).thenReturn(profile);
        Mail mail = new Mail("me@example.com", "Yo", "MyBody");
        when(profile.getActiveDashboardEmailWidget()).thenReturn(mail);

        when(ctx.channel()).thenReturn(channel);

        mailHandler.messageReceived(ctx, user, mailMessage);
        verify(notificationsProcessor).mail(eq("me@example.com"), eq("subj"), eq("body"), eq(1));
        verify(ctx).channel();
    }

    @Test
    public void sendEmptyBodyMailYoNoDefaults() {
        MailMessage mailMessage = (MailMessage) MessageFactory.produce(1, Command.EMAIL, "pupkin@example.com subj body".replaceAll(" ", "\0"));

        when(user.getProfile()).thenReturn(profile);
        Mail mail = new Mail("me@example.com", "Yo", "MyBody");
        when(profile.getActiveDashboardEmailWidget()).thenReturn(mail);

        when(ctx.channel()).thenReturn(channel);

        mailHandler.messageReceived(ctx, user, mailMessage);
        verify(notificationsProcessor).mail(eq("pupkin@example.com"), eq("subj"), eq("body"), eq(1));
        verify(ctx).channel();
    }

}
