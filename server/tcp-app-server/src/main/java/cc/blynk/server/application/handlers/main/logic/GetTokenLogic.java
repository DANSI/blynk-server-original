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
import cc.blynk.utils.TokenGeneratorUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.GET_TOKEN;
import static cc.blynk.server.core.protocol.enums.Response.NOT_ALLOWED;
import static cc.blynk.utils.BlynkByteBufUtil.*;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR_STRING;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetTokenLogic {

    private static final Logger log = LogManager.getLogger(GetTokenLogic.class);

    private final TokenManager tokenManager;
    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;

    public GetTokenLogic(Holder holder) {
        this.tokenManager = holder.tokenManager;
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.dbManager = holder.dbManager;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        if (message.body.contains(BODY_SEPARATOR_STRING)) {
            assignToken(ctx, user, message);
        } else {
            getTokenFlow(ctx, user, message);
        }
    }

    //assigns passed flashed token to existing dashboard
    private void assignToken(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = split2(message.body);

        int dashId = ParseUtil.parseInt(split[0]);
        String token = split[1];
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        blockingIOProcessor.execute(() -> {
            FlashedToken dbFlashedToken = dbManager.selectFlashedToken(token, user.appName);

            if (dbFlashedToken == null) {
                log.error("{} token not exists for app {}.", token, user.appName);
                ctx.writeAndFlush(makeResponse(message.id, NOT_ALLOWED), ctx.voidPromise());
                return;
            }

            if (dbFlashedToken.isActivated) {
                log.error("{} token is already activated for app {}.", token, user.appName);
                ctx.writeAndFlush(makeResponse(message.id, NOT_ALLOWED), ctx.voidPromise());
                return;
            }

            Device device = dash.getDeviceById(dbFlashedToken.deviceId);

            if (device == null) {
                log.error("Device with {} id not exists in dashboards.", dbFlashedToken.deviceId);
                ctx.writeAndFlush(makeResponse(message.id, NOT_ALLOWED), ctx.voidPromise());
                return;
            }

            if (!dbManager.activateFlashedToken(token, user.appName)) {
                log.error("Error activated flashed token {}", token);
                ctx.writeAndFlush(makeResponse(message.id, NOT_ALLOWED), ctx.voidPromise());
                return;
            }

            tokenManager.assignToken(user, dashId, device.id, token);

            ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
        });
    }

    //this is old code and should be removed in future versions.
    //just for back compatibility.
    //todo
    private void getTokenFlow(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashBoardIdString = message.body;

        int dashId = ParseUtil.parseInt(dashBoardIdString);
        int deviceId = 0;

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        Device device = dash.getDeviceById(deviceId);
        String token = device == null ? null : device.token;

        //if token not exists. generate new one
        if (token == null) {
            //todo back compatibility code. remove in future
            dash.devices = new Device[] {
                new Device(deviceId, dash.boardType, dash.boardType)
            };
            //

            token = TokenGeneratorUtil.generateNewToken();
            tokenManager.assignToken(user, dashId, deviceId, token);
        }

        ctx.writeAndFlush(makeASCIIStringMessage(GET_TOKEN, message.id, token), ctx.voidPromise());
    }
}
