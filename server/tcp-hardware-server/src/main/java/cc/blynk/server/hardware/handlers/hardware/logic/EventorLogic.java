package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.eventor.Rule;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.08.16.
 */
public class EventorLogic {

    public static void processEventor(ChannelHandlerContext ctx, DashBoard dash, byte pin, PinType type, String triggerValue) {
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
            if (rule.isValid(pin, type, valueParsed)) {
                for (BaseAction action : rule.actions) {
                    action.execute(ctx, triggerValue);
                }
            }
        }
    }

}
