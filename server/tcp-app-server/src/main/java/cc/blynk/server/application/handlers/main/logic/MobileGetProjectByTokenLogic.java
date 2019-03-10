package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.CopyUtil;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.model.FlashedToken;
import cc.blynk.utils.AppNameUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_TOKEN;
import static cc.blynk.server.internal.CommonByteBufUtil.makeBinaryMessage;
import static cc.blynk.server.internal.CommonByteBufUtil.notAllowed;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class MobileGetProjectByTokenLogic {

    private static final Logger log = LogManager.getLogger(MobileGetProjectByTokenLogic.class);

    private MobileGetProjectByTokenLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       User user, StringMessage message) {
        String token = message.body;

        holder.blockingIOProcessor.executeDB(() -> {
            FlashedToken dbFlashedToken = holder.dbManager.selectFlashedToken(token);

            if (dbFlashedToken == null) {
                log.error("{} token not exists for app {} for {} (GetProject).", token, user.appName, user.email);
                ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
                return;
            }

            User publishUser = holder.userDao.getByName(dbFlashedToken.email, AppNameUtil.BLYNK);

            DashBoard dash = publishUser.profile.getDashById(dbFlashedToken.dashId);
            DashBoard copy = CopyUtil.deepCopy(dash);

            if (copy == null) {
                log.error("Dash with {} id not exists in dashboards.", dbFlashedToken.dashId);
                ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
                return;
            }

            copy.eraseWidgetValues();

            write(ctx, JsonParser.gzipDashRestrictive(copy), message.id);
        });
    }

    public static void write(ChannelHandlerContext ctx, byte[] data, int msgId) {
        if (ctx.channel().isWritable()) {
            var outputMsg = makeBinaryMessage(GET_PROJECT_BY_TOKEN, msgId, data);
            ctx.writeAndFlush(outputMsg, ctx.voidPromise());
        }
    }
}
