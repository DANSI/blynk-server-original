package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.ByteUtils;
import cc.blynk.utils.StringUtils;
import cc.blynk.utils.TokenGeneratorUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_CLONE_CODE;
import static cc.blynk.server.internal.CommonByteBufUtil.energyLimit;
import static cc.blynk.server.internal.CommonByteBufUtil.makeBinaryMessage;
import static cc.blynk.server.internal.CommonByteBufUtil.notAllowed;
import static cc.blynk.server.internal.CommonByteBufUtil.quotaLimit;
import static cc.blynk.server.internal.CommonByteBufUtil.serverError;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class MobileGetProjectByClonedTokenLogic {

    private static final Logger log = LogManager.getLogger(MobileGetProjectByClonedTokenLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;
    private final FileManager fileManager;
    private final TimerWorker timerWorker;
    private final TokenManager tokenManager;
    private final int dashMaxLimit;

    public MobileGetProjectByClonedTokenLogic(Holder holder) {
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.dbManager = holder.dbManager;
        this.fileManager = holder.fileManager;
        this.dashMaxLimit = holder.limits.dashboardsLimit;
        this.timerWorker = holder.timerWorker;
        this.tokenManager = holder.tokenManager;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String token;
        boolean newFlow;
        if (message.body.contains(StringUtils.BODY_SEPARATOR_STRING)) {
            newFlow = true;
            token = split2(message.body)[0];
        } else {
            newFlow = false;
            token = message.body;
        }

        blockingIOProcessor.executeDB(() -> {
            MessageBase result;
            try {
                String json = dbManager.selectClonedProject(token);
                //no cloned project in DB, checking local storage on disk
                if (json == null) {
                    json = fileManager.readClonedProjectFromDisk(token);
                }
                if (json == null) {
                    log.debug("Cannot find request clone QR. {}", token);
                    result = serverError(message.id);
                } else {
                    if (newFlow) {
                        result = createDashboard(user, json, message.id);
                    } else {
                        byte[] data = ByteUtils.compress(json);
                        result = makeBinaryMessage(GET_PROJECT_BY_CLONE_CODE, message.id, data);
                    }
                }
            } catch (Exception e) {
                log.error("Error getting cloned project.", e);
                result = serverError(message.id);
            }
            ctx.writeAndFlush(result, ctx.voidPromise());
        });
    }

    private MessageBase createDashboard(User user, String dashString, int msgId) throws IOException {
        DashBoard newDash = JsonParser.parseDashboard(dashString, msgId);
        newDash.id = max(user.profile.dashBoards) + 1;
        newDash.isPreview = false;
        newDash.parentId = -1;
        newDash.isShared = false;

        if (user.profile.dashBoards.length >= dashMaxLimit) {
            log.debug("Dashboards limit reached.");
            return quotaLimit(msgId);
        }

        for (DashBoard dashBoard : user.profile.dashBoards) {
            if (dashBoard.id == newDash.id) {
                log.debug("Dashboard already exists.");
                return notAllowed(msgId);
            }
        }

        log.info("Creating new cloned dashboard.");

        if (newDash.createdAt == 0) {
            newDash.createdAt = System.currentTimeMillis();
        }

        int price = newDash.energySum();
        if (user.notEnoughEnergy(price)) {
            log.debug("Not enough energy.");
            return energyLimit(msgId);
        }
        user.subtractEnergy(price);
        user.profile.dashBoards = ArrayUtil.add(user.profile.dashBoards, newDash, DashBoard.class);

        if (newDash.devices != null) {
            for (Device device : newDash.devices) {
                String token = TokenGeneratorUtil.generateNewToken();
                tokenManager.assignToken(user, newDash, device, token);
            }
        }

        user.lastModifiedTs = System.currentTimeMillis();

        newDash.addTimers(timerWorker, new UserKey(user));

        byte[] data = ByteUtils.compress(newDash.toString());
        return makeBinaryMessage(GET_PROJECT_BY_CLONE_CODE, msgId, data);
    }

    private int max(DashBoard[] data) {
        int result = 0;
        for (DashBoard dash : data) {
            result = Math.max(result, dash.id);
        }
        return result;
    }
}
