package cc.blynk.client.handlers.hardware;

import cc.blynk.common.enums.Command;
import cc.blynk.common.model.messages.MessageFactory;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.07.15.
 */
@ChannelHandler.Sharable
public class HardwareEchoHandler extends SimpleChannelInboundHandler<HardwareMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HardwareMessage msg) throws Exception {
        if (msg.body.charAt(1) == 'r') {
            StringBuilder sb = new StringBuilder(msg.body);
            sb.setCharAt(1, 'w');
            int random = ThreadLocalRandom.current().nextInt(100);
            sb.append('\0').append(random);
            ctx.writeAndFlush(MessageFactory.produce(msg.id, Command.HARDWARE, sb.toString()));
        }

    }


}
