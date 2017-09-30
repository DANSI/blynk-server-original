package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.REFRESH_TOKEN;
import static cc.blynk.server.internal.BlynkByteBufUtil.makeUTF8StringMessage;

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
        String[] split = StringUtils.split2(message.body);

        int dashId = ParseUtil.parseInt(split[0]);
        int deviceId = 0;

        //new value for multi devices
        if (split.length == 2) {
            deviceId = ParseUtil.parseInt(split[1]);
        }

        User user = state.user;

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        Device device = dash.getDeviceById(deviceId);

        String token = tokenManager.refreshToken(user, dash, device);

        Session session = sessionDao.userSession.get(state.userKey);
        session.closeHardwareChannelByDeviceId(dashId, deviceId);

        if (ctx.channel().isWritable()) {
            ctx.writeAndFlush(makeUTF8StringMessage(REFRESH_TOKEN, message.id, token), ctx.voidPromise());
        }
    }
}
