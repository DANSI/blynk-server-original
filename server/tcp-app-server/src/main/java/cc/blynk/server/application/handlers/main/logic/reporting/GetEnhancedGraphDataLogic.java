package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.GraphPeriod;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.EnhancedHistoryGraph;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NoDataException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.reporting.GraphPinRequest;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.GET_ENHANCED_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Response.NO_DATA;
import static cc.blynk.server.core.protocol.enums.Response.SERVER_ERROR;
import static cc.blynk.utils.BlynkByteBufUtil.makeBinaryMessage;
import static cc.blynk.utils.BlynkByteBufUtil.makeResponse;
import static cc.blynk.utils.ByteUtils.compress;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetEnhancedGraphDataLogic {

    private static final Logger log = LogManager.getLogger(GetEnhancedGraphDataLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final ReportingDao reportingDao;

    public GetEnhancedGraphDataLogic(ReportingDao reportingDao, BlockingIOProcessor blockingIOProcessor) {
        this.reportingDao = reportingDao;
        this.blockingIOProcessor = blockingIOProcessor;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] messageParts = StringUtils.split3(message.body);

        if (messageParts.length < 3) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = Integer.parseInt(messageParts[0]);
        long widgetId = Long.parseLong(messageParts[1]);
        GraphPeriod graphPeriod = GraphPeriod.valueOf(messageParts[2]);

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        Widget widget = dash.getWidgetById(widgetId);

        if (!(widget instanceof EnhancedHistoryGraph)) {
            throw new IllegalCommandException("Passed wrong widget id.");
        }

        EnhancedHistoryGraph enhancedHistoryGraph = (EnhancedHistoryGraph) widget;

        int numberOfStreams = enhancedHistoryGraph.dataStreams.length;
        if (numberOfStreams == 0) {
            log.debug("No data streams for enhanced graph with id {}.", widgetId);
            ctx.writeAndFlush(makeResponse(message.id, NO_DATA), ctx.voidPromise());
            return;
        }

        GraphPinRequest[] requestedPins = new GraphPinRequest[enhancedHistoryGraph.dataStreams.length];

        int i = 0;
        for (GraphDataStream graphDataStream : enhancedHistoryGraph.dataStreams) {
            Target target = dash.getTarget(graphDataStream.targetId);
            int deviceId = target == null ? -1 : target.getDeviceId();
            requestedPins[i] = new GraphPinRequest(dashId, deviceId, graphDataStream.pin, graphPeriod);
            i++;
        }

        readGraphData(ctx.channel(), user, requestedPins, message.id);
    }

    private void readGraphData(Channel channel, User user, GraphPinRequest[] requestedPins, int msgId) {
        blockingIOProcessor.executeHistory(() -> {
            try {
                byte[][] data = reportingDao.getAllFromDisk(user, requestedPins);
                byte[] compressed = compress(requestedPins[0].dashId, data);

                if (channel.isWritable()) {
                    channel.writeAndFlush(makeBinaryMessage(GET_ENHANCED_GRAPH_DATA, msgId, compressed), channel.voidPromise());
                }
            } catch (NoDataException noDataException) {
                channel.writeAndFlush(makeResponse(msgId, NO_DATA), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error reading reporting data. For user {}", user.email);
                channel.writeAndFlush(makeResponse(msgId, SERVER_ERROR), channel.voidPromise());
            }
        });
    }

}
