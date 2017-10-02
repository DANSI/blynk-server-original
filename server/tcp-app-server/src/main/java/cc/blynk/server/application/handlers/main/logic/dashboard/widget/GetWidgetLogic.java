package cc.blynk.server.application.handlers.main.logic.dashboard.widget;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.ParseUtil;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.GET_WIDGET;
import static cc.blynk.server.internal.BlynkByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class GetWidgetLogic {

    private GetWidgetLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = ParseUtil.parseInt(split[0]);
        long widgetId = ParseUtil.parseLong(split[1]);

        User user = state.user;
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        Widget widget = dash.getWidgetByIdOrThrow(widgetId);

        if (ctx.channel().isWritable()) {
            ctx.writeAndFlush(
                    makeUTF8StringMessage(GET_WIDGET, message.id, JsonParser.toJson(widget)),
                    ctx.voidPromise()
            );
        }
    }

}
