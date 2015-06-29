package cc.blynk.server.workers.notifications;

import cc.blynk.server.model.auth.ChannelState;
import cc.blynk.server.model.auth.User;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;


/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.06.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationsProcessorTest {

    @Mock
    private Channel channel;

    @Mock
    private EventLoop eventLoop;

    @Test
    public void testNoCorrectWrapper() {
        when(channel.eventLoop()).thenReturn(eventLoop);
        when(channel.attr(eq(ChannelState.USER))).thenReturn(new Attribute<User>() {
            @Override
            public AttributeKey<User> key() {
                return null;
            }

            @Override
            public User get() {
                return null;
            }

            @Override
            public void set(User user) {

            }

            @Override
            public User getAndSet(User user) {
                return null;
            }

            @Override
            public User setIfAbsent(User user) {
                return null;
            }

            @Override
            public User getAndRemove() {
                return null;
            }

            @Override
            public boolean compareAndSet(User user, User t1) {
                return false;
            }

            @Override
            public void remove() {

            }
        });
        NotificationsProcessor processor = new NotificationsProcessor(5);
        processor.twit(channel, "token", "secret", "body", 1);
        verify(channel, timeout(2000)).eventLoop();
        verify(eventLoop).execute(any());

    }

}
