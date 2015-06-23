package cc.blynk.server.workers.notifications;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
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
        NotificationsProcessor processor = new NotificationsProcessor(5);
        processor.twit(channel, "token", "secret", "body", 1);
        verify(channel, timeout(1000)).eventLoop();
        verify(eventLoop).execute(any());

    }

}
