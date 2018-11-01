package cc.blynk.server.application.handlers.main.logic.dashboard.widget;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.ui.Tabs;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class MobileDeleteWidgetLogic {

    private static final Logger log = LogManager.getLogger(MobileDeleteWidgetLogic.class);

    private MobileDeleteWidgetLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       MobileStateHolder state, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = Integer.parseInt(split[0]);
        long widgetId = Long.parseLong(split[1]);

        User user = state.user;
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        log.debug("Removing widget with id {} for dashId {}.", widgetId, dashId);

        Widget widgetToDelete = null;
        DeviceTiles deviceTiles = null;

        long deviceTilesId = -1;
        long templateId = -1;

        for (Widget widget : dash.widgets) {
            if (widget.id == widgetId) {
                widgetToDelete = widget;
                break;
            }
            if (widget instanceof DeviceTiles) {
                deviceTiles = (DeviceTiles) widget;
                for (TileTemplate tileTemplate : deviceTiles.templates) {
                    for (Widget tileTemplateWidget : tileTemplate.widgets) {
                        if (tileTemplateWidget.id == widgetId) {
                            widgetToDelete = tileTemplateWidget;
                            deviceTilesId = deviceTiles.id;
                            templateId = tileTemplate.id;
                            break;
                        }
                    }
                }
            }
        }

        if (widgetToDelete == null) {
            throw new IllegalCommandException("Widget with passed id not found.");
        }

        user.addEnergy(widgetToDelete.getPrice());
        TimerWorker timerWorker = holder.timerWorker;
        if (deviceTilesId != -1) {
            TileTemplate tileTemplate = deviceTiles.getTileTemplateByIdOrThrow(templateId);
            if (widgetToDelete instanceof Tabs) {
                tileTemplate.widgets = deleteTabs(timerWorker,
                        user, state.userKey, dash.id, deviceTilesId,
                        templateId, tileTemplate.widgets, 0);
            }
            int index = tileTemplate.getWidgetIndexByIdOrThrow(widgetId);
            tileTemplate.widgets = ArrayUtil.remove(tileTemplate.widgets, index, Widget.class);
        } else {
            if (widgetToDelete instanceof Tabs) {
                dash.widgets = deleteTabs(timerWorker, user, state.userKey, dash.id,
                        deviceTilesId, templateId, dash.widgets, 0);
            }
            int index = dash.getWidgetIndexByIdOrThrow(widgetId);
            dash.widgets = ArrayUtil.remove(dash.widgets, index, Widget.class);
        }

        user.profile.cleanPinStorage(dash, widgetToDelete, true);

        if (widgetToDelete instanceof Timer) {
            timerWorker.delete(state.userKey, (Timer) widgetToDelete, dashId, deviceTilesId, templateId);
        } else if (widgetToDelete instanceof Eventor) {
            timerWorker.delete(state.userKey, (Eventor) widgetToDelete, dashId);
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

    /**
     * Removes all widgets with tabId greater than lastTabIndex
     */
    static Widget[] deleteTabs(TimerWorker timerWorker, User user, UserKey userKey,
                               int dashId, long deviceTilesId, long templateId,
                               Widget[] widgets, int lastTabIndex) {
        ArrayList<Widget> zeroTabWidgets = new ArrayList<>();
        int removedWidgetPrice = 0;
        for (Widget widgetToDelete : widgets) {
            if (widgetToDelete.tabId > lastTabIndex) {
                removedWidgetPrice += widgetToDelete.getPrice();
                if (widgetToDelete instanceof Timer) {
                    timerWorker.delete(userKey, (Timer) widgetToDelete, dashId, deviceTilesId, templateId);
                } else if (widgetToDelete instanceof Eventor) {
                    timerWorker.delete(userKey, (Eventor) widgetToDelete, dashId);
                }
            } else {
                zeroTabWidgets.add(widgetToDelete);
            }
        }

        user.addEnergy(removedWidgetPrice);
        return zeroTabWidgets.toArray(new Widget[0]);
    }

}
