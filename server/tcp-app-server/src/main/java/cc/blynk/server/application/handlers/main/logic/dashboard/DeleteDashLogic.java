package cc.blynk.server.application.handlers.main.logic.dashboard;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.utils.BlynkByteBufUtil.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class DeleteDashLogic {

    private static final Logger log = LogManager.getLogger(DeleteDashLogic.class);

    private final TokenManager tokenManager;
    private final TimerWorker timerWorker;
    private final SessionDao sessionDao;

    public DeleteDashLogic(Holder holder) {
        this.tokenManager = holder.tokenManager;
        this.timerWorker = holder.timerWorker;
        this.sessionDao = holder.sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        int dashId = ParseUtil.parseInt(message.body);

        deleteDash(state, dashId);
        state.user.lastModifiedTs = System.currentTimeMillis();

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

    private void deleteDash(AppStateHolder state, int dashId) {
        final User user = state.user;
        int index = user.profile.getDashIndexOrThrow(dashId);

        log.debug("Deleting dashboard {}.", dashId);

        DashBoard dash = user.profile.dashBoards[index];

        user.recycleEnergy(dash.energySum());

        dash.deleteTimers(timerWorker, state.userKey);

        user.profile.dashBoards = ArrayUtil.remove(user.profile.dashBoards, index, DashBoard.class);
        tokenManager.deleteDash(dash);
        Session session = sessionDao.userSession.get(state.userKey);
        session.closeHardwareChannelByDashId(dashId);
    }

}
