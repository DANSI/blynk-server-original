package cc.blynk.server.application.handlers.main.logic.dashboard.widget;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.energyLimit;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class CreateWidgetLogic {

    private static final Logger log = LogManager.getLogger(CreateWidgetLogic.class);

    private CreateWidgetLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       AppStateHolder state, StringMessage message) {
        //format is "dashId widget_json" or "dashId widgetId templateId widget_json"
        var split = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        var dashId = Integer.parseInt(split[0]);

        long widgetAddToId;
        long templateIdAddToId;
        String widgetString;
        if (split.length == 4) {
            widgetAddToId = Long.parseLong(split[1]);
            templateIdAddToId = Long.parseLong(split[2]);
            widgetString = split[3];
        } else {
            widgetAddToId = -1;
            templateIdAddToId = -1;
            widgetString = split[1];
        }

        if (widgetString == null || widgetString.isEmpty()) {
            throw new IllegalCommandException("Income widget message is empty.");
        }

        if (widgetString.length() > holder.limits.widgetSizeLimitBytes) {
            throw new NotAllowedException("Widget is larger then limit.", message.id);
        }

        var user = state.user;
        var dash = user.profile.getDashByIdOrThrow(dashId);

        var newWidget = JsonParser.parseWidget(widgetString, message.id);

        if (newWidget.width < 1 || newWidget.height < 1) {
            throw new NotAllowedException("Widget has wrong dimensions.", message.id);
        }

        log.debug("Creating new widget {} for dashId {}.", widgetString, dashId);

        for (var widget : dash.widgets) {
            if (widget.id == newWidget.id) {
                throw new NotAllowedException("Widget with same id already exists.", message.id);
            }
            if (widget instanceof DeviceTiles) {
                Widget widgetInTiles = ((DeviceTiles) widget).getWidgetById(newWidget.id);
                if (widgetInTiles != null) {
                    throw new NotAllowedException("Widget with same id already exists.", message.id);
                }
            }
        }

        var price = newWidget.getPrice();
        if (user.notEnoughEnergy(price)) {
            log.debug("Not enough energy.");
            ctx.writeAndFlush(energyLimit(message.id), ctx.voidPromise());
            return;
        }
        user.subtractEnergy(price);

        //widget could be added to project or to other widget like DeviceTiles
        if (widgetAddToId == -1) {
            dash.widgets = ArrayUtil.add(dash.widgets, newWidget, Widget.class);
        } else {
            //right now we can only add to DeviceTiles widget
            var deviceTiles = (DeviceTiles) dash.getWidgetByIdOrThrow(widgetAddToId);
            var tileTemplate = deviceTiles.getTileTemplateByIdOrThrow(templateIdAddToId);
            tileTemplate.widgets = ArrayUtil.add(tileTemplate.widgets, newWidget, Widget.class);
        }

        dash.cleanPinStorage(newWidget, true);
        user.lastModifiedTs = dash.updatedAt;

        TimerWorker timerWorker = holder.timerWorker;
        if (newWidget instanceof Timer) {
            timerWorker.add(state.userKey, (Timer) newWidget, dashId, widgetAddToId, templateIdAddToId);
        } else if (newWidget instanceof Eventor) {
            timerWorker.add(state.userKey, (Eventor) newWidget, dashId);
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
