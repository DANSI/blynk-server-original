package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.TokenGeneratorUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.GET_CLONE_CODE;
import static cc.blynk.utils.BlynkByteBufUtil.makeASCIIStringMessage;
import static cc.blynk.utils.BlynkByteBufUtil.serverError;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetCloneCodeLogic {

    private static final Logger log = LogManager.getLogger(GetCloneCodeLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;

    public GetCloneCodeLogic(Holder holder) {
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.dbManager = holder.dbManager;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        int dashId = ParseUtil.parseInt(message.body);

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        String token = TokenGeneratorUtil.generateNewToken();
        String json = JsonParser.toJsonRestrictiveDashboard(dash);

        blockingIOProcessor.executeDB(() -> {
            ByteBuf result;
            try {
                boolean insertStatus = dbManager.insertClonedProject(token, json);
                if (insertStatus) {
                    result = makeASCIIStringMessage(GET_CLONE_CODE, message.id, token);
                } else {
                    log.error("Clone project insert in DB failed for {}", user.email);
                    result = serverError(message.id);
                }
            } catch (Exception e) {
                log.error("Error cloning project.", e);
                result = serverError(message.id);
            }
            ctx.writeAndFlush(result, ctx.voidPromise());
        });
    }
}
