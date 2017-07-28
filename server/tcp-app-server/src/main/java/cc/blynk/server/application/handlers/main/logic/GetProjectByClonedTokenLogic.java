package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.utils.ByteUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.utils.BlynkByteBufUtil.makeBinaryMessage;
import static cc.blynk.utils.BlynkByteBufUtil.serverError;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetProjectByClonedTokenLogic {

    private static final Logger log = LogManager.getLogger(GetProjectByClonedTokenLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;

    public GetProjectByClonedTokenLogic(Holder holder) {
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.dbManager = holder.dbManager;
    }

    public void messageReceived(ChannelHandlerContext ctx, StringMessage message) {
        String token = message.body;

        blockingIOProcessor.executeDB(() -> {
            ByteBuf result;
            try {
                String json = dbManager.selectClonedProject(token);
                byte[] data = ByteUtils.compress(json);
                result = makeBinaryMessage(LOAD_PROFILE_GZIPPED, message.id, data);
            } catch (Exception e) {
                log.error("Error cloning project.", e);
                result = serverError(message.id);
            }
            ctx.writeAndFlush(result, ctx.voidPromise());
        });
    }
}
