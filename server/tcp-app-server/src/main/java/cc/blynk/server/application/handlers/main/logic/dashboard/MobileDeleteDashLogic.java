package cc.blynk.server.application.handlers.main.logic.dashboard;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class MobileDeleteDashLogic {

    private static final Logger log = LogManager.getLogger(MobileDeleteDashLogic.class);

    private MobileDeleteDashLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       MobileStateHolder state, StringMessage message) {
        var dashId = Integer.parseInt(message.body);

        deleteDash(holder, state, dashId);
        state.user.lastModifiedTs = System.currentTimeMillis();

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

    private static void deleteDash(Holder holder, MobileStateHolder state, int dashId) {
        User user = state.user;
        int index = user.profile.getDashIndexOrThrow(dashId);

        log.debug("Deleting dashboard {}.", dashId);

        DashBoard dash = user.profile.dashBoards[index];

        user.addEnergy(dash.energySum());

        holder.timerWorker.deleteTimers(state.userKey, dash);
        holder.tokenManager.deleteDash(dash);
        holder.sessionDao.closeHardwareChannelByDashId(state.userKey, dashId);
        holder.reportScheduler.cancelStoredFuture(user, dashId);

        holder.blockingIOProcessor.executeHistory(() -> {
            for (Device device : dash.devices) {
                try {
                    holder.reportingDiskDao.delete(state.user, dashId, device.id);
                } catch (Exception e) {
                    log.warn("Error removing device data. Reason : {}.", e.getMessage());
                }
            }
        });

        user.profile.dashBoards = ArrayUtil.remove(user.profile.dashBoards, index, DashBoard.class);
    }

}
