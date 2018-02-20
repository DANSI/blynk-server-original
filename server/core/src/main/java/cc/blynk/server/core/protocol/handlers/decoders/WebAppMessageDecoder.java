package cc.blynk.server.core.protocol.handlers.decoders;

import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.stats.GlobalStats;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 11.01.16.
 */
@ChannelHandler.Sharable
public class WebAppMessageDecoder extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(WebAppMessageDecoder.class);

    private final GlobalStats stats;

    public WebAppMessageDecoder(GlobalStats globalStats) {
        this.stats = globalStats;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("In webappdecoder. {}", msg);
        if (msg instanceof BinaryWebSocketFrame) {
            try {
                ByteBuf in = ((BinaryWebSocketFrame) msg).content();

                short command = in.readUnsignedByte();
                int messageId = in.readUnsignedShort();

                MessageBase message;
                if (command == Command.RESPONSE) {
                    message = new ResponseMessage(messageId, (int) in.readUnsignedInt());
                } else {
                    int codeOrLength = (int) in.readUnsignedInt();
                    message = produce(messageId, command, (String) in.readCharSequence(codeOrLength, UTF_8));
                }

                log.trace("Incoming websocket msg {}", message);
                //stats.markWebSocketAndBlynk(command);
                ctx.fireChannelRead(message);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof WebSocketHandshakeException) {
            log.debug("Web Socket Handshake Exception.", cause);
        }
    }

}
