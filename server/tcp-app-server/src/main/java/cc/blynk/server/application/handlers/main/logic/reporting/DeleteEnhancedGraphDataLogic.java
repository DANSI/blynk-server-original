package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.outputs.graph.EnhancedHistoryGraph;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.utils.BlynkByteBufUtil.ok;

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
        String[] messageParts = StringUtils.split3(message.body);

        if (messageParts.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = Integer.parseInt(messageParts[0]);
        long widgetId = Long.parseLong(messageParts[1]);
        int streamIndex = -1;
        if (message.length == 3) {
            streamIndex = Integer.parseInt(messageParts[2]);
        }

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        EnhancedHistoryGraph enhancedHistoryGraph = (EnhancedHistoryGraph) dash.getWidgetById(widgetId);

        if (streamIndex == -1 || streamIndex > enhancedHistoryGraph.dataStreams.length - 1) {
            delete(ctx.channel(), message.id, user, dash, enhancedHistoryGraph.dataStreams);
        } else {
            delete(ctx.channel(), message.id, user, dash, enhancedHistoryGraph.dataStreams[streamIndex]);
        }
    }

    private void delete(Channel channel, int msgId, User user, DashBoard dash, GraphDataStream... dataStreams) {
        blockingIOProcessor.executeHistory(() -> {
            try {
                for (GraphDataStream graphDataStream : dataStreams) {
                    Target target = dash.getTarget(graphDataStream.targetId);
                    DataStream dataStream = graphDataStream.dataStream;
                    if (target != null && dataStream != null) {
                        int deviceId = target.getDeviceId();
                        reportingDao.delete(user, dash.id, deviceId, dataStream.pinType, dataStream.pin);
                    }
                }
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (NumberFormatException e) {
                throw new IllegalCommandException("HardwareLogic command body incorrect.");
            }
        });
    }
}
