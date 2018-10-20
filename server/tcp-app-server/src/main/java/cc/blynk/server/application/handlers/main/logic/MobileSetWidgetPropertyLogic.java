package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommandBody;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * Handler that allows to change widget properties from hardware side.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class MobileSetWidgetPropertyLogic {

    private static final Logger log = LogManager.getLogger(MobileSetWidgetPropertyLogic.class);

    private MobileSetWidgetPropertyLogic(SessionDao sessionDao) {
    }

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        var splitBody = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        if (splitBody.length != 4) {
            log.debug("AppSetWidgetProperty command body has wrong format. {}", message.body);
            ctx.writeAndFlush(illegalCommandBody(message.id), ctx.voidPromise());
            return;
        }

        var dashId = Integer.parseInt(splitBody[0]);
        var widgetId = Long.parseLong(splitBody[1]);
        var property = splitBody[2];
        var propertyValue = splitBody[3];

        if (property.length() == 0 || propertyValue.length() == 0) {
            log.debug("AppSetWidgetProperty command body has wrong format. {}", message.body);
            ctx.writeAndFlush(illegalCommandBody(message.id), ctx.voidPromise());
            return;
        }

        var widgetProperty = WidgetProperty.getProperty(property);

        if (widgetProperty == null) {
            log.debug("Unsupported app set property {}.", property);
            ctx.writeAndFlush(illegalCommandBody(message.id), ctx.voidPromise());
            return;
        }

        var dash = user.profile.getDashByIdOrThrow(dashId);
        //for now supporting only virtual pins
        var widget = dash.getWidgetById(widgetId);
        if (widget == null) {
            widget = dash.getWidgetByIdInDeviceTilesOrThrow(widgetId);
        }

        if (!widget.setProperty(widgetProperty, propertyValue)) {
            log.debug("Property {} with value {} not supported.", property, propertyValue);
            ctx.writeAndFlush(illegalCommandBody(message.id), ctx.voidPromise());
            return;
        }

        dash.updatedAt = System.currentTimeMillis();
        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
