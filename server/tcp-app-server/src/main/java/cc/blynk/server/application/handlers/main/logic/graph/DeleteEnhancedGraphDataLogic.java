package cc.blynk.server.application.handlers.main.logic.graph;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.outputs.graph.EnhancedHistoryGraph;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
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
public class DeleteEnhancedGraphDataLogic {

    private static final Logger log = LogManager.getLogger(DeleteEnhancedGraphDataLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final ReportingDao reportingDao;

    public DeleteEnhancedGraphDataLogic(ReportingDao reportingDao, BlockingIOProcessor blockingIOProcessor) {
        this.reportingDao = reportingDao;
        this.blockingIOProcessor = blockingIOProcessor;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        var messageParts = StringUtils.split3(message.body);

        if (messageParts.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        var dashIdAndDeviceId = split2Device(messageParts[0]);
        var dashId = Integer.parseInt(dashIdAndDeviceId[0]);
        var widgetId = Long.parseLong(messageParts[1]);
        var streamIndex = -1;
        if (message.body.length() == 3) {
            streamIndex = Integer.parseInt(messageParts[2]);
        }
        var targetId = -1;
        if (dashIdAndDeviceId.length == 2) {
            targetId = Integer.parseInt(dashIdAndDeviceId[1]);
        }

        var dash = user.profile.getDashByIdOrThrow(dashId);

        var widget = dash.getWidgetById(widgetId);
        if (widget == null) {
            widget = dash.getWidgetByIdInDeviceTilesOrThrow(widgetId);
        }
        var enhancedHistoryGraph = (EnhancedHistoryGraph) widget;

        if (streamIndex == -1 || streamIndex > enhancedHistoryGraph.dataStreams.length - 1) {
            delete(ctx.channel(), message.id, user, dash, targetId, enhancedHistoryGraph.dataStreams);
        } else {
            delete(ctx.channel(), message.id, user, dash, targetId, enhancedHistoryGraph.dataStreams[streamIndex]);
        }
    }

    private void delete(Channel channel, int msgId, User user, DashBoard dash, int targetId,
                        GraphDataStream... dataStreams) {
        blockingIOProcessor.executeHistory(() -> {
            try {
                for (GraphDataStream graphDataStream : dataStreams) {
                    var target = dash.getTarget(graphDataStream.getTargetId(targetId));
                    var dataStream = graphDataStream.dataStream;
                    if (target != null && dataStream != null && dataStream.pinType != null) {
                        var deviceId = target.getDeviceId();
                        reportingDao.delete(user, dash.id, deviceId, dataStream.pinType, dataStream.pin);
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
