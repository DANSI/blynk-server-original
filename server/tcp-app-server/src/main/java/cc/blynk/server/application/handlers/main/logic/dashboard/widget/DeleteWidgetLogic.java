package cc.blynk.server.application.handlers.main.logic.dashboard.widget;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.ui.Tabs;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.ParseUtil;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

import static cc.blynk.server.internal.BlynkByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public class DeleteWidgetLogic {

    private static final Logger log = LogManager.getLogger(DeleteWidgetLogic.class);

    private final TimerWorker timerWorker;

    public DeleteWidgetLogic(TimerWorker timerWorker) {
        this.timerWorker = timerWorker;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = ParseUtil.parseInt(split[0]);
        long widgetId = ParseUtil.parseLong(split[1]);

        User user = state.user;
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        log.debug("Removing widget with id {} for dashId {}.", widgetId, dashId);

        Widget widgetToDelete = null;
        boolean inDeviceTiles = false;
        DeviceTiles deviceTiles = null;

        for (Widget widget : dash.widgets) {
            if (widget.id == widgetId) {
                widgetToDelete = widget;
                break;
            }
            if (widget instanceof DeviceTiles) {
                deviceTiles = ((DeviceTiles) widget);
                widgetToDelete = deviceTiles.getWidgetById(widgetId);
                if (widgetToDelete != null) {
                    inDeviceTiles = true;
                    break;
                }
            }
        }

        if (widgetToDelete == null) {
            throw new IllegalCommandException("Widget with passed id not found.");
        }

        if (widgetToDelete instanceof Tabs) {
            deleteTabs(timerWorker, user, state.userKey, dash, 0);
        }

        user.recycleEnergy(widgetToDelete.getPrice());
        if (inDeviceTiles) {
            deviceTiles.deleteWidget(widgetId);
        } else {
            int index = dash.getWidgetIndexByIdOrThrow(widgetId);
            dash.widgets = ArrayUtil.remove(dash.widgets, index, Widget.class);
        }

        dash.updatedAt = System.currentTimeMillis();

        if (widgetToDelete instanceof Timer) {
            timerWorker.delete(state.userKey, (Timer) widgetToDelete, dashId);
        } else if (widgetToDelete instanceof Eventor) {
            timerWorker.delete(state.userKey, (Eventor) widgetToDelete, dashId);
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

    /**
     * Removes all widgets with tabId greater than lastTabIndex
     */
    static void deleteTabs(TimerWorker timerWorker, User user, UserKey userKey,
                                  DashBoard dash, int lastTabIndex) {
        ArrayList<Widget> zeroTabWidgets = new ArrayList<>();
        int removedWidgetPrice = 0;
        for (Widget widgetToDelete : dash.widgets) {
            if (widgetToDelete.tabId > lastTabIndex) {
                removedWidgetPrice += widgetToDelete.getPrice();
                if (widgetToDelete instanceof Timer) {
                    timerWorker.delete(userKey, (Timer) widgetToDelete, dash.id);
                } else if (widgetToDelete instanceof Eventor) {
                    timerWorker.delete(userKey, (Eventor) widgetToDelete, dash.id);
                }
            } else {
                zeroTabWidgets.add(widgetToDelete);
            }
        }

        user.recycleEnergy(removedWidgetPrice);
        dash.widgets = zeroTabWidgets.toArray(new Widget[zeroTabWidgets.size()]);
        dash.updatedAt = System.currentTimeMillis();
    }

}
