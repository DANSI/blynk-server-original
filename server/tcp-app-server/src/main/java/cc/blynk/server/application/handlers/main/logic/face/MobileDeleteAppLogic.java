package cc.blynk.server.application.handlers.main.logic.face;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class MobileDeleteAppLogic {

    private final TokenManager tokenManager;
    private final TimerWorker timerWorker;
    private final SessionDao sessionDao;

    public MobileDeleteAppLogic(Holder holder) {
        this.tokenManager = holder.tokenManager;
        this.timerWorker = holder.timerWorker;
        this.sessionDao = holder.sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, MobileStateHolder state, StringMessage message) {
        var id = message.body;

        var user = state.user;

        var existingAppIndex = user.profile.getAppIndexById(id);

        if (existingAppIndex == -1) {
            throw new NotAllowedException("App with passed is not exists.", message.id);
        }

        var projectIds = user.profile.apps[existingAppIndex].projectIds;

        var result = new ArrayList<DashBoard>();
        for (DashBoard dash : user.profile.dashBoards) {
            if (ArrayUtil.contains(projectIds, dash.id)) {
                timerWorker.deleteTimers(state.userKey, dash);
                tokenManager.deleteDash(dash);
                sessionDao.closeHardwareChannelByDashId(state.userKey, dash.id);
            } else {
                result.add(dash);
            }
        }

        user.profile.dashBoards = result.toArray(new DashBoard[0]);
        user.profile.apps = ArrayUtil.remove(user.profile.apps, existingAppIndex, App.class);
        user.lastModifiedTs = System.currentTimeMillis();

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
