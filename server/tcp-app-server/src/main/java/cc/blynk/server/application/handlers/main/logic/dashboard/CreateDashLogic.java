package cc.blynk.server.application.handlers.main.logic.dashboard;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.exceptions.QuotaLimitException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.EmptyArraysUtil;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.StringUtils;
import cc.blynk.utils.TokenGeneratorUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.energyLimit;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class CreateDashLogic {

    private static final Logger log = LogManager.getLogger(CreateDashLogic.class);

    private final int dashMaxLimit;
    private final int dashMaxSize;
    private final TimerWorker timerWorker;
    private final TokenManager tokenManager;

    public CreateDashLogic(TimerWorker timerWorker, TokenManager tokenManager, int dashMaxLimit, int dashMaxSize) {
        this.tokenManager = tokenManager;
        this.dashMaxLimit = dashMaxLimit;
        this.dashMaxSize = dashMaxSize;
        this.timerWorker = timerWorker;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        var generateTokensForDevices = true;
        final String dashString;
        if (message.body.startsWith("no_token")) {
            generateTokensForDevices = false;
            dashString = StringUtils.split2(message.body)[1];
        } else {
            dashString = message.body;
        }

        if (dashString == null || dashString.isEmpty()) {
            throw new IllegalCommandException("Income create dash message is empty.");
        }

        if (dashString.length() > dashMaxSize) {
            throw new NotAllowedException("User dashboard is larger then limit.", message.id);
        }

        log.debug("Trying to parse user newDash : {}", dashString);
        var newDash = JsonParser.parseDashboard(dashString, message.id);

        var user = state.user;
        if (user.profile.dashBoards.length >= dashMaxLimit) {
            throw new QuotaLimitException("Dashboards limit reached.", message.id);
        }

        for (var dashBoard : user.profile.dashBoards) {
            if (dashBoard.id == newDash.id) {
                throw new NotAllowedException("Dashboard already exists.", message.id);
            }
        }

        log.info("Creating new dashboard.");

        if (newDash.createdAt == 0) {
            newDash.createdAt = System.currentTimeMillis();
        }

        int price = newDash.energySum();
        if (user.notEnoughEnergy(price)) {
            log.debug("Not enough energy.");
            ctx.writeAndFlush(energyLimit(message.id), ctx.voidPromise());
            return;
        }
        user.subtractEnergy(price);
        user.profile.dashBoards = ArrayUtil.add(user.profile.dashBoards, newDash, DashBoard.class);

        if (newDash.devices == null) {
            newDash.devices = EmptyArraysUtil.EMPTY_DEVICES;
        } else {
            for (var device : newDash.devices) {
                //this case only possible for clone,
                device.erase();
                if (generateTokensForDevices) {
                    String token = TokenGeneratorUtil.generateNewToken();
                    tokenManager.assignToken(user, newDash, device, token);
                }
            }
        }

        user.lastModifiedTs = System.currentTimeMillis();

        newDash.addTimers(timerWorker, state.userKey);

        if (!generateTokensForDevices) {
            newDash.eraseValues();
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
