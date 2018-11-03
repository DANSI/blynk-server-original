package cc.blynk.server.application.handlers.main.logic.graph;

import cc.blynk.server.Holder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.Superchart;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommand;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2Device;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class MobileDeleteEnhancedGraphDataLogic {

    private static final Logger log = LogManager.getLogger(MobileDeleteEnhancedGraphDataLogic.class);

    private MobileDeleteEnhancedGraphDataLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       User user, StringMessage message) {
        String[] messageParts = StringUtils.split3(message.body);

        if (messageParts.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        String[] dashIdAndDeviceId = split2Device(messageParts[0]);
        int dashId = Integer.parseInt(dashIdAndDeviceId[0]);
        long widgetId = Long.parseLong(messageParts[1]);
        int streamIndex = -1;
        if (message.body.length() == 3) {
            streamIndex = Integer.parseInt(messageParts[2]);
        }
        int targetId = -1;
        if (dashIdAndDeviceId.length == 2) {
            targetId = Integer.parseInt(dashIdAndDeviceId[1]);
        }

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        Widget widget = dash.getWidgetById(widgetId);
        if (widget == null) {
            widget = dash.getWidgetByIdInDeviceTilesOrThrow(widgetId);
        }
        Superchart enhancedHistoryGraph = (Superchart) widget;

        if (streamIndex == -1 || streamIndex > enhancedHistoryGraph.dataStreams.length - 1) {
            delete(holder, ctx.channel(), message.id, user, dash, targetId, enhancedHistoryGraph.dataStreams);
        } else {
            delete(holder, ctx.channel(),
                    message.id, user, dash, targetId, enhancedHistoryGraph.dataStreams[streamIndex]);
        }
    }

    private static void delete(Holder holder, Channel channel, int msgId,
                               User user, DashBoard dash, int targetId, GraphDataStream... dataStreams) {
        holder.blockingIOProcessor.executeHistory(() -> {
            try {
                for (GraphDataStream graphDataStream : dataStreams) {
                    Target target;
                    int targetIdUpdated = graphDataStream.getTargetId(targetId);
                    if (targetIdUpdated < Tag.START_TAG_ID) {
                        target = user.profile.getDeviceById(dash, targetIdUpdated);
                    } else if (targetIdUpdated < DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
                        target = user.profile.getTagById(dash, targetIdUpdated);
                    } else {
                        target = dash.getDeviceSelector(targetIdUpdated);
                    }

                    DataStream dataStream = graphDataStream.dataStream;
                    if (target != null && dataStream != null && dataStream.pinType != null) {
                        int deviceId = target.getDeviceId();
                        holder.reportingDiskDao.delete(user, dash.id, deviceId, dataStream.pinType, dataStream.pin);
                    }
                }
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.debug("Error removing enhanced graph data. Reason : {}.", e.getMessage());
                channel.writeAndFlush(illegalCommand(msgId), channel.voidPromise());
            }
        });
    }
}
