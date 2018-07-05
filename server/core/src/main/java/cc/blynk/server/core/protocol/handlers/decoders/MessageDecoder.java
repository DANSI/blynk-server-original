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
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;

/**
 * Decodes input byte array into java message.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class MessageDecoder extends ByteToMessageDecoder {

    private static final Logger log = LogManager.getLogger(MessageDecoder.class);

    private final GlobalStats stats;
    private final QuotaLimitChecker limitChecker;

    public MessageDecoder(GlobalStats stats, Limits limits) {
        this.stats = stats;
        this.limitChecker = new QuotaLimitChecker(limits.userQuotaLimit);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 5) {
            return;
        }

        in.markReaderIndex();

        short command = in.readUnsignedByte();
        int messageId = in.readUnsignedShort();
        int codeOrLength = in.readUnsignedShort();

        if (limitChecker.quotaReached(ctx, messageId)) {
            return;
        }

        MessageBase message;
        if (command == Command.RESPONSE) {
            message = new ResponseMessage(messageId, codeOrLength);
        } else {
            if (in.readableBytes() < codeOrLength) {
                in.resetReaderIndex();
                return;
            }

            message = produce(messageId, command, (String) in.readCharSequence(codeOrLength, CharsetUtil.UTF_8));
        }

        log.trace("Incoming {}", message);

        stats.mark(command);

        out.add(message);
    }

    public InstanceLoadMeter getQuotaMeter() {
        return limitChecker.quotaMeter;
    }

}
