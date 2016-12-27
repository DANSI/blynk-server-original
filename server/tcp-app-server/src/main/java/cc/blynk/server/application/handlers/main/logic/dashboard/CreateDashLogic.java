package cc.blynk.server.application.handlers.main.logic.dashboard;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.exceptions.QuotaLimitException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.JsonParser;
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
public class CreateDashLogic {

    private static final Logger log = LogManager.getLogger(CreateDashLogic.class);

    private final int DASH_MAX_LIMIT;
    private final int DASH_MAX_SIZE;
    private final TimerWorker timerWorker;

    public CreateDashLogic(TimerWorker timerWorker, int dashMaxLimit, int dashMaxSize) {
        this.DASH_MAX_LIMIT = dashMaxLimit;
        this.DASH_MAX_SIZE = dashMaxSize;
        this.timerWorker = timerWorker;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        String dashString = message.body;

        if (dashString == null || dashString.equals("")) {
            throw new IllegalCommandException("Income create dash message is empty.");
        }

        if (dashString.length() > DASH_MAX_SIZE) {
            throw new NotAllowedException("User dashboard is larger then limit.");
        }

        log.debug("Trying to parse user newDash : {}", dashString);
        DashBoard newDash = JsonParser.parseDashboard(dashString);

        //todo this may be removed later. or generate device tokens
        newDash.devices = new Device[0];

        log.info("Creating new dashboard.");

        final User user = state.user;
        if (user.profile.dashBoards.length >= DASH_MAX_LIMIT) {
            throw new QuotaLimitException("Dashboards limit reached.");
        }

        for (DashBoard dashBoard : user.profile.dashBoards) {
            if (dashBoard.id == newDash.id) {
                throw new NotAllowedException("Dashboard already exists.");
            }
        }

        if (newDash.createdAt == 0) {
            newDash.createdAt = System.currentTimeMillis();
        }

        user.subtractEnergy(newDash.energySum());
        user.profile.dashBoards = ArrayUtil.add(user.profile.dashBoards, newDash, DashBoard.class);
        user.lastModifiedTs = System.currentTimeMillis();

        for (Widget widget : newDash.widgets) {
            if (widget instanceof Timer) {
                timerWorker.add(state.userKey, (Timer) widget, newDash.id);
            }
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
