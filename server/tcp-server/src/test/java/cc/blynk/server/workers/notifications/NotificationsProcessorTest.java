package cc.blynk.server.workers.notifications;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.handlers.hardware.HardwareHandler;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
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
    private ServerProperties props;

    @Mock
    private EventLoop eventLoop;

    @Mock
    private ReportingDao reportingDao;

    @Test
    public void testNoCorrectWrapper() {
        HardwareHandler hardwareHandler = new HardwareHandler(props, null, null, null, new HardwareStateHolder(1, new User("test", "test"), null));

        when(channel.eventLoop()).thenReturn(eventLoop);
        when(channel.pipeline()).thenReturn(pipeline);

        when(pipeline.get(HardwareHandler.class)).thenReturn(hardwareHandler);

        BlockingIOProcessor processor = new BlockingIOProcessor(5, "", reportingDao);
        processor.twit(channel, "token", "secret", "body", 1);
        verify(channel, timeout(2000)).eventLoop();
        verify(eventLoop, timeout(2000)).execute(any());

    }

}
