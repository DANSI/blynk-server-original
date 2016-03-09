package cc.blynk.server.application.handlers.main.logic.dashboard.widget;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.utils.StringUtils.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public class CreateWidgetLogic {

    private static final Logger log = LogManager.getLogger(CreateWidgetLogic.class);

    private final int MAX_WIDGET_SIZE;

    public CreateWidgetLogic(int maxWidgetSize) {
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

        DashBoard dash = user.profile.getDashById(dashId, message.id);

        Widget newWidget = JsonParser.parseWidget(widgetString, message.id);

        log.debug("Creating new widget {}.", widgetString);

        for (Widget widget : dash.widgets) {
            if (widget.id == newWidget.id) {
                throw new NotAllowedException("Widget with same id already exists.", message.id);
            }
        }

        dash.widgets = ArrayUtil.add(dash.widgets, newWidget);
        dash.updatedAt = System.currentTimeMillis();
        user.lastModifiedTs = dash.updatedAt;

        ctx.writeAndFlush(new ResponseMessage(message.id, OK), ctx.voidPromise());
    }

}
