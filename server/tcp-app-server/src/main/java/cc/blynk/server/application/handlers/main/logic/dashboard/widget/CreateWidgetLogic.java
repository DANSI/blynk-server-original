package cc.blynk.server.application.handlers.main.logic.dashboard.widget;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.ParseUtil;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.BlynkByteBufUtil.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public class CreateWidgetLogic {

    private static final Logger log = LogManager.getLogger(CreateWidgetLogic.class);

    private final int maxWidgetSize;
    private final TimerWorker timerWorker;

    public CreateWidgetLogic(int maxWidgetSize, TimerWorker timerWorker) {
        this.maxWidgetSize = maxWidgetSize;
        this.timerWorker = timerWorker;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        //format is "dashId widget_json" or "dashId widgetId templateId widget_json"
        String[] split = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = ParseUtil.parseInt(split[0]);

        long widgetAddToId;
        long templateIdAddToId;
        String widgetString;
        if (split.length == 4) {
            widgetAddToId = ParseUtil.parseLong(split[1]);
            templateIdAddToId = ParseUtil.parseLong(split[2]);
            widgetString = split[3];
        } else {
            widgetAddToId = -1;
            templateIdAddToId = -1;
            widgetString = split[1];
        }

        if (widgetString == null || widgetString.isEmpty()) {
            throw new IllegalCommandException("Income widget message is empty.");
        }

        if (widgetString.length() > maxWidgetSize) {
            throw new NotAllowedException("Widget is larger then limit.");
        }

        User user = state.user;
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        Widget newWidget = JsonParser.parseWidget(widgetString);

        if (newWidget.width < 1 || newWidget.height < 1) {
            throw new NotAllowedException("Widget has wrong dimensions.");
        }

        log.debug("Creating new widget {}.", widgetString);

        for (Widget widget : dash.widgets) {
            if (widget.id == newWidget.id) {
                throw new NotAllowedException("Widget with same id already exists.");
            }
            if (widget instanceof DeviceTiles) {
                ((DeviceTiles) widget).checkForSameWidgetId(newWidget.id);
            }
        }

        user.subtractEnergy(newWidget.getPrice());

        //widget could be added to project or to other widget like DeviceTiles
        if (widgetAddToId == -1) {
            dash.widgets = ArrayUtil.add(dash.widgets, newWidget, Widget.class);
        } else {
            //right now we can only add to DeviceTiles widget
            DeviceTiles deviceTiles = (DeviceTiles) dash.getWidgetByIdOrThrow(widgetAddToId);
            TileTemplate tileTemplate = deviceTiles.getTileTemplateByIdOrThrow(templateIdAddToId);
            tileTemplate.widgets = ArrayUtil.add(tileTemplate.widgets, newWidget, Widget.class);
        }

        dash.cleanPinStorage(newWidget, true);
        dash.updatedAt = user.lastModifiedTs;

        if (newWidget instanceof Timer) {
            timerWorker.add(state.userKey, (Timer) newWidget, dashId);
        } else if (newWidget instanceof Eventor) {
            timerWorker.add(state.userKey, (Eventor) newWidget, dashId);
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
