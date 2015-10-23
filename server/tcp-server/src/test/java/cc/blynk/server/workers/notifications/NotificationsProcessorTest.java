package cc.blynk.server.workers.notifications;

import cc.blynk.server.handlers.hardware.HardwareHandler;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.model.auth.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
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
    private ChannelPipeline pipeline;

    @Mock
    private HardwareHandler hardwareHandler;

    @Mock
    private EventLoop eventLoop;

    @Test
    public void testNoCorrectWrapper() {
        when(channel.eventLoop()).thenReturn(eventLoop);
        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.last()).thenReturn(hardwareHandler);
        when(hardwareHandler.getHandlerState()).thenReturn(new HandlerState(new User("test", "test")));
        BlockingIOProcessor processor = new BlockingIOProcessor(5, "");
        processor.twit(channel, "token", "secret", "body", 1);
        verify(channel, timeout(2000)).eventLoop();
        verify(eventLoop, timeout(2000)).execute(any());

    }

}
