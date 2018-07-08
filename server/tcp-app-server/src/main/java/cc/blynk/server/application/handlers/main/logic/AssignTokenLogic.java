package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.notAllowed;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

/**
 * Assigns static generated token to assigned device.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class AssignTokenLogic {

    private static final Logger log = LogManager.getLogger(AssignTokenLogic.class);

    private AssignTokenLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       User user, StringMessage message) {
        var split = split2(message.body);

        var dashId = Integer.parseInt(split[0]);
        var token = split[1];
        var dash = user.profile.getDashByIdOrThrow(dashId);

        DBManager dbManager = holder.dbManager;
        TokenManager tokenManager = holder.tokenManager;
        holder.blockingIOProcessor.executeDB(() -> {
            var dbFlashedToken = dbManager.selectFlashedToken(token);

            if (dbFlashedToken == null) {
                log.error("{} token not exists for app {}.", token, user.appName);
                ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
                return;
            }

            if (dbFlashedToken.isActivated) {
                log.error("{} token is already activated for app {}.", token, user.appName);
                ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
                return;
            }

            var device = dash.getDeviceById(dbFlashedToken.deviceId);

            if (device == null) {
                log.error("Device with {} id not exists in dashboards.", dbFlashedToken.deviceId);
                ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
                return;
            }

            if (!dbManager.activateFlashedToken(token)) {
                log.error("Error activated flashed token {}", token);
                ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
                return;
            }

            tokenManager.assignToken(user, dash, device, token);

            ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
        });
    }

}
