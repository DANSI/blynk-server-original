package cc.blynk.server.application.handlers.main.logic.dashboard;

import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DashboardSettings;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.StringUtils;
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
public final class MobileUpdateDashSettingLogic {

    private static final Logger log = LogManager.getLogger(MobileUpdateDashSettingLogic.class);

    private MobileUpdateDashSettingLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, MobileStateHolder state,
                                       StringMessage message, int settingsSizeLimit) {
        String[] split = StringUtils.split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = Integer.parseInt(split[0]);
        String dashSettingsString = split[1];

        if (dashSettingsString == null || dashSettingsString.isEmpty()) {
            throw new IllegalCommandException("Income dash settings message is empty.");
        }

        if (dashSettingsString.length() > settingsSizeLimit) {
            throw new NotAllowedException("User dashboard setting message is larger then limit.", message.id);
        }

        log.debug("Trying to parse project settings : {}", dashSettingsString);
        DashboardSettings settings = JsonParser.parseDashboardSettings(dashSettingsString, message.id);

        User user = state.user;
        DashBoard existingDash = user.profile.getDashByIdOrThrow(dashId);

        existingDash.updateSettings(settings);
        user.lastModifiedTs = existingDash.updatedAt;

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
