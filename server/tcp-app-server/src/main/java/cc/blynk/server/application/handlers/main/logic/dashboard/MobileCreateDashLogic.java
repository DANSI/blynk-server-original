package cc.blynk.server.application.handlers.main.logic.dashboard;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.exceptions.QuotaLimitException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.EmptyArraysUtil;
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
public final class MobileCreateDashLogic {

    private static final Logger log = LogManager.getLogger(MobileCreateDashLogic.class);

    private MobileCreateDashLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       MobileStateHolder state, StringMessage message) {
        boolean generateTokensForDevices = true;
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

        if (dashString.length() > holder.limits.profileSizeLimitBytes) {
            throw new NotAllowedException("User dashboard is larger then limit.", message.id);
        }

        log.debug("Trying to parse user newDash : {}", dashString);
        DashBoard newDash = JsonParser.parseDashboard(dashString, message.id);

        User user = state.user;
        if (user.profile.dashBoards.length >= holder.limits.dashboardsLimit) {
            throw new QuotaLimitException("Dashboards limit reached.", message.id);
        }

        for (DashBoard dashBoard : user.profile.dashBoards) {
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
            for (Device device : newDash.devices) {
                //this case only possible for clone,
                device.erase();
                if (generateTokensForDevices) {
                    String token = TokenGeneratorUtil.generateNewToken();
                    holder.tokenManager.assignToken(user, newDash, device, token);
                }
            }
        }

        user.lastModifiedTs = System.currentTimeMillis();

        newDash.addTimers(holder.timerWorker, state.userKey);

        if (!generateTokensForDevices) {
            newDash.eraseWidgetValues();
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
