package cc.blynk.server.application.handlers.main.logic.dashboard;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.workers.timer.TimerWorker;
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
public final class UpdateDashLogic {

    private static final Logger log = LogManager.getLogger(UpdateDashLogic.class);

    private UpdateDashLogic() {
    }

    //todo should accept only dash info and ignore widgets. should be fixed after migration
    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       AppStateHolder state, StringMessage message) {
        var dashString = message.body;

        if (dashString == null || dashString.isEmpty()) {
            throw new IllegalCommandException("Income create dash message is empty.");
        }

        if (dashString.length() > holder.limits.profileSizeLimitBytes) {
            throw new NotAllowedException("User dashboard is larger then limit.", message.id);
        }

        log.debug("Trying to parse user dash : {}", dashString);
        var updatedDash = JsonParser.parseDashboard(dashString, message.id);

        if (updatedDash == null) {
            throw new IllegalCommandException("Project parsing error.");
        }

        log.debug("Saving dashboard.");

        var user = state.user;

        var existingDash = user.profile.getDashByIdOrThrow(updatedDash.id);

        TimerWorker timerWorker = holder.timerWorker;
        timerWorker.deleteTimers(state.userKey, existingDash);
        updatedDash.addTimers(timerWorker, state.userKey);

        existingDash.updateFields(updatedDash);
        user.lastModifiedTs = existingDash.updatedAt;

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
