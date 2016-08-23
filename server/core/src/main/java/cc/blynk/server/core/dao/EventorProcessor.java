package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.eventor.Rule;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.SetPin;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.notification.NotificationAction;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;

/**
 * Class responsible for handling eventor logic.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.08.16.
 */
public class EventorProcessor {

    public void processEventor(ChannelHandlerContext ctx, Session session, DashBoard dash, byte pin, PinType type, String triggerValue) {
        Eventor eventor = dash.getWidgetByType(Eventor.class);
        if (eventor == null || eventor.rules == null) {
            return;
        }

        double valueParsed;
        try {
            valueParsed = Double.parseDouble(triggerValue);
        } catch (NumberFormatException nfe) {
            return;
        }

        for (Rule rule : eventor.rules) {
            if (rule.isReady(pin, type)) {
                if (rule.isValid(valueParsed)) {
                    if (!rule.isProcessed) {
                        for (BaseAction action : rule.actions) {
                            if (action instanceof SetPin) {
                                execute(session, dash.id, (SetPin) action);
                            } else if (action instanceof NotificationAction) {
                                execute(ctx, triggerValue, (NotificationAction) action);
                            }

                            rule.isProcessed = true;
                        }
                    }
                } else {
                    rule.isProcessed = false;
                }
            }
        }
    }

    private static void execute(ChannelHandlerContext ctx, String triggerValue, NotificationAction notificationAction) {
        execute(ctx, notificationAction.message, notificationAction.makeMessage(triggerValue));
    }
    private static void execute(ChannelHandlerContext ctx, String message, StringMessage stringMessage) {
        if (message != null && !message.isEmpty()) {
            ctx.pipeline().fireChannelRead(stringMessage);
        }
    }

    private static void execute(Session session, int dashId, SetPin action) {
        execute(session, dashId, action.pin, action.value);
    }
    private static void execute(Session session, int dashId, Pin pin, String value) {
        if (pin != null && pin.pinType != null && pin.pin > -1 && value != null) {
            String body = Pin.makeHardwareBody(pin.pwmMode, pin.pinType, pin.pin, value);
            session.sendMessageToHardware(dashId, HARDWARE, 888, body);
        }
    }
}
