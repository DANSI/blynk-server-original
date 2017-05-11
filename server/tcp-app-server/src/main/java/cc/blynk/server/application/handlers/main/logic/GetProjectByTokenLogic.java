package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.db.model.FlashedToken;
import cc.blynk.utils.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_TOKEN;
import static cc.blynk.server.core.protocol.enums.Response.NOT_ALLOWED;
import static cc.blynk.utils.BlynkByteBufUtil.makeBinaryMessage;
import static cc.blynk.utils.BlynkByteBufUtil.makeResponse;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetProjectByTokenLogic {

    private static final Logger log = LogManager.getLogger(GetProjectByTokenLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;

    public GetProjectByTokenLogic(Holder holder) {
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.dbManager = holder.dbManager;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String token = message.body;

        blockingIOProcessor.executeDB(() -> {
            FlashedToken dbFlashedToken = dbManager.selectFlashedToken(token);

            if (dbFlashedToken == null) {
                log.error("{} token not exists for app {}.", token, user.appName);
                ctx.writeAndFlush(makeResponse(message.id, NOT_ALLOWED), ctx.voidPromise());
                return;
            }

            DashBoard dash = user.profile.getDashById(dbFlashedToken.dashId);

            if (dash == null) {
                log.error("Dash with {} id not exists in dashboards.", dbFlashedToken.dashId);
                ctx.writeAndFlush(makeResponse(message.id, NOT_ALLOWED), ctx.voidPromise());
                return;
            }

            write(ctx, JsonParser.gzipDashRestrictive(dash), message.id);
        });
    }

    public static void write(ChannelHandlerContext ctx, byte[] data, int msgId) {
        if (ctx.channel().isWritable()) {
            ByteBuf outputMsg = makeBinaryMessage(GET_PROJECT_BY_TOKEN, msgId, data);
            ctx.writeAndFlush(outputMsg, ctx.voidPromise());
        }
    }
}
