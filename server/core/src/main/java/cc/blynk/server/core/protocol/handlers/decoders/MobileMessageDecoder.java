package cc.blynk.server.core.protocol.handlers.decoders;

import cc.blynk.server.Limits;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.exceptions.UnsupportedCommandException;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.core.stats.metrics.InstanceLoadMeter;
import cc.blynk.server.internal.QuotaLimitChecker;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
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
 * Created on 18/1/2018.
 */
public class MobileMessageDecoder extends ByteToMessageDecoder {

    private static final Logger log = LogManager.getLogger(MobileMessageDecoder.class);

    private final GlobalStats stats;
    public static final int PROTOCOL_APP_HEADER_SIZE = 7;
    private static final DecoderException decoderException =
            new DecoderException(new UnsupportedCommandException("Length field is wrong.", 1));
    private final QuotaLimitChecker limitChecker;

    public MobileMessageDecoder(GlobalStats stats, Limits limits) {
        this.stats = stats;
        this.limitChecker = new QuotaLimitChecker(limits.userQuotaLimit);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < PROTOCOL_APP_HEADER_SIZE) {
            return;
        }

        in.markReaderIndex();

        short command = in.readUnsignedByte();
        int messageId = in.readUnsignedShort();
        //actually here should be long. but we do not expect this number to be large
        //so it should perfectly fit int
        int codeOrLength = (int) in.readUnsignedInt();

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

            validateLength(codeOrLength);

            message = produce(messageId, command, (String) in.readCharSequence(codeOrLength, CharsetUtil.UTF_8));
        }

        log.trace("Incoming {}", message);

        stats.mark(command);

        out.add(message);
    }

    private static void validateLength(int length) {
        if (length < 0 || length > 10_000_000) {
            throw decoderException;
        }
    }

    public InstanceLoadMeter getQuotaMeter() {
        return limitChecker.quotaMeter;
    }
}
