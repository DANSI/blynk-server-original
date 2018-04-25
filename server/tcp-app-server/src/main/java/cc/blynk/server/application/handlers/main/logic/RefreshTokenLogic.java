package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.REFRESH_TOKEN;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class RefreshTokenLogic {

    private final TokenManager tokenManager;
    private final SessionDao sessionDao;

    public RefreshTokenLogic(Holder holder) {
        this.tokenManager = holder.tokenManager;
        this.sessionDao = holder.sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        var split = StringUtils.split2(message.body);

        var dashId = Integer.parseInt(split[0]);
        var deviceId = 0;

        //new value for multi devices
        if (split.length == 2) {
            deviceId = Integer.parseInt(split[1]);
        }

        var user = state.user;

        var dash = user.profile.getDashByIdOrThrow(dashId);
        var device = dash.getDeviceById(deviceId);

        var token = tokenManager.refreshToken(user, dash, device);

        var session = sessionDao.userSession.get(state.userKey);
        session.closeHardwareChannelByDeviceId(dashId, deviceId);

        if (ctx.channel().isWritable()) {
            ctx.writeAndFlush(makeUTF8StringMessage(REFRESH_TOKEN, message.id, token), ctx.voidPromise());
        }
    }
}
