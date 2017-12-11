package cc.blynk.server.application.handlers.main.logic.dashboard.widget;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.ParseUtil;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.BlynkByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

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

        log.debug("Creating new widget {}.", widgetString);

        for (Widget widget : dash.widgets) {
            if (widget.id == newWidget.id) {
                throw new NotAllowedException("Widget with same id already exists.");
            }
        }

        user.subtractEnergy(newWidget.getPrice());
        dash.widgets = ArrayUtil.add(dash.widgets, newWidget, Widget.class);
        dash.cleanPinStorage(newWidget, true);
        dash.updatedAt = System.currentTimeMillis();

        user.lastModifiedTs = dash.updatedAt;

        if (newWidget instanceof Timer) {
            timerWorker.add(state.userKey, (Timer) newWidget, dashId);
        } else if (newWidget instanceof Eventor) {
            timerWorker.add(state.userKey, (Eventor) newWidget, dashId);
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
