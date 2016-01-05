package cc.blynk.server.workers.notifications;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.hardware.handlers.hardware.HardwareHandler;
import cc.blynk.utils.ServerProperties;
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

        when(pipeline.get(BaseSimpleChannelInboundHandler.class)).thenReturn(hardwareHandler);

        BlockingIOProcessor processor = new BlockingIOProcessor(5, "", reportingDao);
        processor.twit(channel, "token", "secret", "body", 1);
        verify(channel, timeout(2000)).eventLoop();
        verify(eventLoop, timeout(2000)).execute(any());

    }

}
