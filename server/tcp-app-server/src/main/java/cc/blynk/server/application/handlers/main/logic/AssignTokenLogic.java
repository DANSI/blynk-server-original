package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.db.model.FlashedToken;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.utils.BlynkByteBufUtil.notAllowed;
import static cc.blynk.utils.BlynkByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

/**
 * Assigns static generated token to assigned device.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class AssignTokenLogic {

    private static final Logger log = LogManager.getLogger(AssignTokenLogic.class);

    private final TokenManager tokenManager;
    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;

    public AssignTokenLogic(Holder holder) {
        this.tokenManager = holder.tokenManager;
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.dbManager = holder.dbManager;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = split2(message.body);

        int dashId = ParseUtil.parseInt(split[0]);
        String token = split[1];
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        blockingIOProcessor.executeDB(() -> {
            FlashedToken dbFlashedToken = dbManager.selectFlashedToken(token);

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

            Device device = dash.getDeviceById(dbFlashedToken.deviceId);

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

            tokenManager.assignToken(user, dashId, device.id, token);

            ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
        });
    }

}
