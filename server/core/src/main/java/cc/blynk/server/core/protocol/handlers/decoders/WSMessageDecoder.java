package cc.blynk.server.core.protocol.handlers.decoders;

import cc.blynk.server.Limits;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.core.stats.metrics.InstanceLoadMeter;
import cc.blynk.server.internal.QuotaLimitChecker;
import io.netty.buffer.ByteBuf;
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
public class WSMessageDecoder extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(WSMessageDecoder.class);

    private final GlobalStats stats;
    private final QuotaLimitChecker limitChecker;

    public WSMessageDecoder(GlobalStats globalStats, Limits limits) {
        this.stats = globalStats;
        this.limitChecker = new QuotaLimitChecker(limits.userQuotaLimit);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("In webappdecoder. {}", msg);
        if (msg instanceof BinaryWebSocketFrame) {
            try {
                ByteBuf in = ((BinaryWebSocketFrame) msg).content();

                short command = in.readUnsignedByte();
                int messageId = in.readUnsignedShort();

                if (limitChecker.quotaReached(ctx, messageId)) {
                    return;
                }

                MessageBase message;
                if (command == Command.RESPONSE) {
                    message = new ResponseMessage(messageId, (int) in.readUnsignedInt());
                } else {
                    int codeOrLength = in.capacity() - 3;
                    message = produce(messageId, command, (String) in.readCharSequence(codeOrLength, UTF_8));
                }

                log.trace("Incoming websocket msg {}", message);
                stats.markWithoutGlobal(Command.WEB_SOCKETS);
                ctx.fireChannelRead(message);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof WebSocketHandshakeException) {
            log.debug("Web Socket Handshake Exception.", cause);
        }
    }

    public InstanceLoadMeter getQuotaMeter() {
        return limitChecker.quotaMeter;
    }

}
