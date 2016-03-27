package cc.blynk.server.application.handlers.main.logic.dashboard.widget;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.utils.ByteBufUtil.*;
import static cc.blynk.utils.StringUtils.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public class DeleteWidgetLogic {

    private static final Logger log = LogManager.getLogger(DeleteWidgetLogic.class);

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = message.body.split(BODY_SEPARATOR_STRING, 2);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.", message.id);
        }

        int dashId = ParseUtil.parseInt(split[0], message.id) ;
        long widgetId = ParseUtil.parseLong(split[1], message.id);

        DashBoard dash = user.profile.getDashById(dashId, message.id);
        int existingWidgetIndex = dash.getWidgetIndex(widgetId, message.id);

        log.debug("Removing widget with id {}.", widgetId);

        delete(user, dash, existingWidgetIndex);

        ctx.writeAndFlush(ok(ctx, message.id), ctx.voidPromise());
    }

    private static void delete(User user, DashBoard dash, int existingWidgetIndex) {
        user.recycleEnergy(dash.widgets[existingWidgetIndex].getPrice());
        dash.widgets = ArrayUtil.remove(dash.widgets, existingWidgetIndex);
        dash.updatedAt = System.currentTimeMillis();
        user.lastModifiedTs = dash.updatedAt;
    }

}
