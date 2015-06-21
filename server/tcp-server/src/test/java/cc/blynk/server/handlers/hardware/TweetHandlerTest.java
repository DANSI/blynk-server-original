package cc.blynk.server.handlers.hardware;

import cc.blynk.common.enums.Command;
import cc.blynk.common.model.messages.MessageFactory;
import cc.blynk.common.model.messages.protocol.hardware.TweetMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.TestBase;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.NotificationBodyInvalidException;
import cc.blynk.server.exceptions.QuotaLimitException;
import cc.blynk.server.model.Profile;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Twitter;
import cc.blynk.server.notifications.twitter.exceptions.NotificationNotAuthorizedException;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * The Blynk Project.
 * Created by Andrew Zakordonets.
 * Created on 26.04.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class TweetHandlerTest extends TestBase {

	@Mock
	private static ServerProperties props;

	@Mock
	private NotificationsProcessor notificationsProcessor;

	@Mock
	private ChannelHandlerContext ctx;

	@Mock
	private UserRegistry userRegistry;

	@Mock
	private SessionsHolder sessionsHolder;

	@Mock
	private TweetHandler tweetHandler;

	@Mock
	private User user;

	@Mock
	private Profile profile;

	@Test(expected = NotificationBodyInvalidException.class)
	public void testTweetMessageWithEmptyBody() {
		TweetMessage tweetMessage = (TweetMessage) MessageFactory.produce(1, Command.TWEET, "");
		when(user.getProfile()).thenReturn(profile);
		TweetHandler tweetHandler = new TweetHandler(props, userRegistry, sessionsHolder, notificationsProcessor);
		tweetHandler.messageReceived(ctx, user, tweetMessage);
	}

	@Test(expected = NotificationBodyInvalidException.class)
	public void testTweetMessageWithBodyMoreThen140Symbols() {
		final String longBody = RandomStringUtils.random(150);
		TweetMessage tweetMessage = (TweetMessage) MessageFactory.produce(1, Command.TWEET, longBody);
		when(user.getProfile()).thenReturn(profile);
		TweetHandler tweetHandler = new TweetHandler(props, userRegistry, sessionsHolder, notificationsProcessor);
		tweetHandler.messageReceived(ctx, user, tweetMessage);
	}

	@Test(expected = NotificationNotAuthorizedException.class)
	public void testTweetMessageWithNoTwitterAccessToken() {
		TweetMessage tweetMessage = (TweetMessage) MessageFactory.produce(1, Command.TWEET, "test tweet");
		when(user.getProfile()).thenReturn(profile);
		TweetHandler tweetHandler = new TweetHandler(props, userRegistry, sessionsHolder, notificationsProcessor);
		when(user.getProfile()).thenReturn(profile);
		tweetHandler.messageReceived(ctx, user, tweetMessage);
	}

	@Test(expected = NotificationNotAuthorizedException.class)
	public void testTweetMessageWithTwitterTokenNull() {
		TweetMessage tweetMessage = (TweetMessage) MessageFactory.produce(1, Command.TWEET, "test tweet");
		when(user.getProfile()).thenReturn(profile);
		TweetHandler tweetHandler = new TweetHandler(props, userRegistry, sessionsHolder, notificationsProcessor);
		when(user.getProfile()).thenReturn(profile);
		Twitter twitter = new Twitter();
		twitter.token = null;
		twitter.secret = "secret_token";
		when(user.getProfile().getActiveDashboardWidgetByType(Twitter.class)).thenReturn(twitter);
		tweetHandler.messageReceived(ctx, user, tweetMessage);
	}

	@Test(expected = NotificationNotAuthorizedException.class)
	public void testTweetMessageWithTwitterTokenEmpty() {
		TweetMessage tweetMessage = (TweetMessage) MessageFactory.produce(1, Command.TWEET, "test tweet");
		when(user.getProfile()).thenReturn(profile);
		TweetHandler tweetHandler = new TweetHandler(props, userRegistry, sessionsHolder, notificationsProcessor);
		when(user.getProfile()).thenReturn(profile);
		Twitter twitter = new Twitter();
		twitter.token = null;
		twitter.secret = "secret_token";
		when(user.getProfile().getActiveDashboardWidgetByType(Twitter.class)).thenReturn(twitter);
		tweetHandler.messageReceived(ctx, user, tweetMessage);
	}

	@Test(expected = NotificationNotAuthorizedException.class)
	public void testTweetMessageWithTwitterSecretTokenNull() {
		TweetMessage tweetMessage = (TweetMessage) MessageFactory.produce(1, Command.TWEET, "test tweet");
		when(user.getProfile()).thenReturn(profile);
		TweetHandler tweetHandler = new TweetHandler(props, userRegistry, sessionsHolder, notificationsProcessor);
		when(user.getProfile()).thenReturn(profile);
		Twitter twitter = new Twitter();
		twitter.token = "token";
		twitter.secret = null;
		when(user.getProfile().getActiveDashboardWidgetByType(Twitter.class)).thenReturn(twitter);
		tweetHandler.messageReceived(ctx, user, tweetMessage);
	}

	@Test(expected = NotificationNotAuthorizedException.class)
	public void testTweetMessageWithTwitterSecretTokenEmpty() {
		TweetMessage tweetMessage = (TweetMessage) MessageFactory.produce(1, Command.TWEET, "test tweet");
		when(user.getProfile()).thenReturn(profile);
		TweetHandler tweetHandler = new TweetHandler(props, userRegistry, sessionsHolder, notificationsProcessor);
		when(user.getProfile()).thenReturn(profile);
		Twitter twitter = new Twitter();
		twitter.token = "token";
		twitter.secret = null;
		when(user.getProfile().getActiveDashboardWidgetByType(Twitter.class)).thenReturn(twitter);
		tweetHandler.messageReceived(ctx, user, tweetMessage);
	}

	@Test(expected = QuotaLimitException.class)
	public void testSendQuotaLimitationException() throws InterruptedException {
		TweetMessage tweetMessage = (TweetMessage) MessageFactory.produce(1, Command.TWEET, "this is a test tweet");
		User user = spy(new User());
		TweetHandler tweetHandler = spy(new TweetHandler(new ServerProperties(), userRegistry, sessionsHolder, notificationsProcessor));
		when(user.getProfile()).thenReturn(profile);
		Twitter twitter = new Twitter();
		twitter.token = "token";
		twitter.secret = "secret_token";
		when(user.getProfile().getActiveDashboardWidgetByType(Twitter.class)).thenReturn(twitter);
		tweetHandler.messageReceived(ctx, user, tweetMessage);
		tweetHandler.messageReceived(ctx, user, tweetMessage);
	}

	@Test()
	public void testSendQuotaLimitationIsWorking() throws InterruptedException {
		TweetMessage tweetMessage = (TweetMessage) MessageFactory.produce(1, Command.TWEET, "this is a test tweet");
		ServerProperties props = new ServerProperties();
		props.setProperty("notifications.frequency.user.quota.limit", "1");
		final long defaultQuotaTime = props.getLongProperty("notifications.frequency.user.quota.limit") * 1000;
		User user = spy(new User());
		TweetHandler tweetHandler = spy(new TweetHandler(props, userRegistry, sessionsHolder, notificationsProcessor));
		when(user.getProfile()).thenReturn(profile);
		Twitter twitter = new Twitter();
		twitter.token = "token";
		twitter.secret = "secret_token";
		when(user.getProfile().getActiveDashboardWidgetByType(Twitter.class)).thenReturn(twitter);
		tweetHandler.messageReceived(ctx, user, tweetMessage);
		TimeUnit.MILLISECONDS.sleep(defaultQuotaTime);
		tweetHandler.messageReceived(ctx, user, tweetMessage);
	}

}
