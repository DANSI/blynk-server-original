package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.utils.ByteUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_CLONE_CODE;
import static cc.blynk.server.internal.BlynkByteBufUtil.makeBinaryMessage;
import static cc.blynk.server.internal.BlynkByteBufUtil.serverError;

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
    private final FileManager fileManager;

    public GetProjectByClonedTokenLogic(Holder holder) {
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.dbManager = holder.dbManager;
        this.fileManager = holder.fileManager;
    }

    public void messageReceived(ChannelHandlerContext ctx, StringMessage message) {
        String token = message.body;

        blockingIOProcessor.executeDB(() -> {
            ByteBuf result;
            try {
                String json = dbManager.selectClonedProject(token);
                //no cloned project in DB, checking local storage on disk
                if (json == null) {
                    json = fileManager.readClonedProjectFromDisk(token);
                }
                byte[] data = ByteUtils.compress(json);
                result = makeBinaryMessage(GET_PROJECT_BY_CLONE_CODE, message.id, data);
            } catch (Exception e) {
                log.error("Error getting cloned project.", e);
                result = serverError(message.id);
            }
            ctx.writeAndFlush(result, ctx.voidPromise());
        });
    }
}
