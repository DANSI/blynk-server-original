package cc.blynk.server.application.handlers.main.logic.dashboard.widget;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.ui.Tabs;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.JsonParser;
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
public class UpdateWidgetLogic {

    private static final Logger log = LogManager.getLogger(UpdateWidgetLogic.class);

    private final int MAX_WIDGET_SIZE;

    public UpdateWidgetLogic(int maxWidgetSize) {
        this.MAX_WIDGET_SIZE = maxWidgetSize;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = message.body.split(BODY_SEPARATOR_STRING, 2);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.", message.id);
        }

        int dashId = ParseUtil.parseInt(split[0], message.id) ;
        String widgetString = split[1];

        if (widgetString == null || widgetString.equals("")) {
            throw new IllegalCommandException("Income widget message is empty.", message.id);
        }

        if (widgetString.length() > MAX_WIDGET_SIZE) {
            throw new NotAllowedException("Widget is larger then limit.", message.id);
        }

        DashBoard dash = user.profile.getDashById(dashId);

        Widget newWidget = JsonParser.parseWidget(widgetString, message.id);

        log.debug("Updating widget {}.", widgetString);

        if (dash == null || newWidget == null) {
            log.error("Error updating widget {}. Project : {}", widgetString, dash);
            throw new IllegalCommandException("Empty widget or project.", message.id);
        }

        int existingWidgetIndex = dash.getWidgetIndex(newWidget.id, message.id);

        if (newWidget instanceof Tabs) {
            Tabs newTabs = (Tabs) newWidget;
            DeleteWidgetLogic.deleteTabs(user, dash, newTabs.tabs.length - 1);
        }

        dash.widgets[existingWidgetIndex] = newWidget;
        dash.updatedAt = System.currentTimeMillis();
        user.lastModifiedTs = dash.updatedAt;

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
