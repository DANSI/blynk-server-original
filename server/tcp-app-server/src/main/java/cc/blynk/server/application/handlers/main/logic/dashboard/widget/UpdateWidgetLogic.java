package cc.blynk.server.application.handlers.main.logic.dashboard.widget;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.ui.Tabs;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.ParseUtil;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public class UpdateWidgetLogic {

    private static final Logger log = LogManager.getLogger(UpdateWidgetLogic.class);

    private final int maxWidgetSize;
    private final TimerWorker timerWorker;

    public UpdateWidgetLogic(int maxWidgetSize, TimerWorker timerWorker) {
        this.maxWidgetSize = maxWidgetSize;
        this.timerWorker = timerWorker;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = ParseUtil.parseInt(split[0]);
        String widgetString = split[1];

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

        log.debug("Updating widget {}.", widgetString);

        Widget prevWidget = null;
        boolean inDeviceTiles = false;
        DeviceTiles deviceTiles = null;

        long widgetId = newWidget.id;
        for (Widget widget : dash.widgets) {
            if (widget.id == widgetId) {
                prevWidget = widget;
                break;
            }
            if (widget instanceof DeviceTiles) {
                deviceTiles = ((DeviceTiles) widget);
                prevWidget = deviceTiles.getWidgetById(widgetId);
                if (prevWidget != null) {
                    inDeviceTiles = true;
                    break;
                }
            }
        }

        if (prevWidget == null) {
            throw new IllegalCommandException("Widget with passed id not found.");
        }

        if (newWidget instanceof Tabs) {
            Tabs newTabs = (Tabs) newWidget;
            DeleteWidgetLogic.deleteTabs(timerWorker, user, state.userKey, dash, newTabs.tabs.length - 1);
        }

        if (prevWidget instanceof Notification && newWidget instanceof Notification) {
            Notification prevNotif = (Notification) prevWidget;
            Notification newNotif = (Notification) newWidget;
            newNotif.iOSTokens.putAll(prevNotif.iOSTokens);
            newNotif.androidTokens.putAll(prevNotif.androidTokens);
        }

        //do not update template, tile fields for DeviceTiles.
        if (newWidget instanceof DeviceTiles && prevWidget instanceof DeviceTiles) {
            DeviceTiles prevDeviceTiles = (DeviceTiles) prevWidget;
            DeviceTiles newDeviceTiles = (DeviceTiles) newWidget;
            newDeviceTiles.tiles = prevDeviceTiles.tiles;
            newDeviceTiles.templates = prevDeviceTiles.templates;
        }

        if (inDeviceTiles) {
            deviceTiles.updateWidget(newWidget);
        } else {
            dash.widgets = ArrayUtil.copyAndReplace(
                    dash.widgets, newWidget, dash.getWidgetIndexByIdOrThrow(newWidget.id));
        }

        dash.cleanPinStorage(newWidget, true);
        dash.updatedAt = System.currentTimeMillis();
        user.lastModifiedTs = dash.updatedAt;

        if (prevWidget instanceof Timer) {
            timerWorker.delete(state.userKey, (Timer) prevWidget, dashId);
        } else if (prevWidget instanceof Eventor) {
            timerWorker.delete(state.userKey, (Eventor) prevWidget, dashId);
        }

        if (newWidget instanceof Timer) {
            timerWorker.add(state.userKey, (Timer) newWidget, dashId);
        } else if (newWidget instanceof Eventor) {
            timerWorker.add(state.userKey, (Eventor) newWidget, dashId);
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
