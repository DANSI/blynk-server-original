package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.protocol.hardware.HardwareInfoMessage;
import cc.blynk.server.TestBase;
import cc.blynk.server.handlers.hardware.logic.HardwareInfoLogic;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HardwareInfoLogicTest extends TestBase {

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private ChannelPipeline pipeline;

    @Test
    public void testCorrectBehavior() {
        HardwareInfoLogic logic = new HardwareInfoLogic(props);
        when(ctx.pipeline()).thenReturn(pipeline);
        HardwareInfoMessage hardwareInfoLogic = new HardwareInfoMessage(1, "ver 0.3.2-beta h-beat 60 buff-in 256 dev ESP8266".replaceAll(" ", "\0"));
        logic.messageReceived(ctx, null, hardwareInfoLogic);

        verify(pipeline).remove(ReadTimeoutHandler.class);
        verify(pipeline).addFirst(any(ReadTimeoutHandler.class));
        verify(ctx).writeAndFlush(produce(1, OK));
    }

}
