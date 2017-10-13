package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.EnhancedHistoryGraph;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphPeriod;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NoDataException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.reporting.GraphPinRequest;
import cc.blynk.server.internal.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.GET_ENHANCED_GRAPH_DATA;
import static cc.blynk.server.internal.BlynkByteBufUtil.makeBinaryMessage;
import static cc.blynk.server.internal.BlynkByteBufUtil.noData;
import static cc.blynk.server.internal.BlynkByteBufUtil.serverError;
import static cc.blynk.utils.ByteUtils.compress;
import static cc.blynk.utils.StringUtils.split2Device;

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
        String[] messageParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        if (messageParts.length < 3) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int targetId = -1;
        String[] dashIdAndTargetIdString = split2Device(messageParts[0]);
        if (dashIdAndTargetIdString.length == 2) {
            targetId = ParseUtil.parseInt(dashIdAndTargetIdString[1]);
        }
        int dashId = Integer.parseInt(dashIdAndTargetIdString[0]);

        long widgetId = Long.parseLong(messageParts[1]);
        GraphPeriod graphPeriod = GraphPeriod.valueOf(messageParts[2]);
        int page = 0;
        if (messageParts.length == 4) {
            page = Integer.parseInt(messageParts[3]);
        }
        int skipCount = graphPeriod.numberOfPoints * page;

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        Widget widget = dash.getWidgetById(widgetId);

        //special case for device tiles widget.
        if (widget == null) {
            DeviceTiles deviceTiles = dash.getWidgetByType(DeviceTiles.class);
            if (deviceTiles != null) {
                widget = deviceTiles.getWidgetById(widgetId);
            }
        }


        if (!(widget instanceof EnhancedHistoryGraph)) {
            throw new IllegalCommandException("Passed wrong widget id.");
        }

        EnhancedHistoryGraph enhancedHistoryGraph = (EnhancedHistoryGraph) widget;

        int numberOfStreams = enhancedHistoryGraph.dataStreams.length;
        if (numberOfStreams == 0) {
            log.debug("No data streams for enhanced graph with id {}.", widgetId);
            ctx.writeAndFlush(noData(message.id), ctx.voidPromise());
            return;
        }

        GraphPinRequest[] requestedPins = new GraphPinRequest[enhancedHistoryGraph.dataStreams.length];

        int i = 0;
        for (GraphDataStream graphDataStream : enhancedHistoryGraph.dataStreams) {
            //special case, for device tiles widget targetID may be overrided
            Target target = dash.getTarget(graphDataStream.getTargetId(targetId));
            if (target == null) {
                requestedPins[i] = new GraphPinRequest(dashId, -1,
                        graphDataStream.dataStream, graphPeriod, skipCount, graphDataStream.functionType);
            } else {
                if (target.isTag()) {
                    requestedPins[i] = new GraphPinRequest(dashId, target.getDeviceIds(),
                            graphDataStream.dataStream, graphPeriod, skipCount, graphDataStream.functionType);
                } else {
                    requestedPins[i] = new GraphPinRequest(dashId, target.getDeviceId(),
                            graphDataStream.dataStream, graphPeriod, skipCount, graphDataStream.functionType);
                }
            }
            i++;
        }

        readGraphData(ctx.channel(), user, requestedPins, message.id);
    }

    private void readGraphData(Channel channel, User user, GraphPinRequest[] requestedPins, int msgId) {
        blockingIOProcessor.executeHistory(() -> {
            try {
                byte[][] data = reportingDao.getReportingData(user, requestedPins);
                byte[] compressed = compress(requestedPins[0].dashId, data);

                if (compressed.length > Short.MAX_VALUE * 2) {
                    log.error("Data set for history graph is too large {}, for {}.", compressed.length, user.email);
                    channel.writeAndFlush(serverError(msgId), channel.voidPromise());
                } else {
                    if (channel.isWritable()) {
                        channel.writeAndFlush(
                                makeBinaryMessage(GET_ENHANCED_GRAPH_DATA, msgId, compressed),
                                channel.voidPromise()
                        );
                    }
                }
            } catch (NoDataException noDataException) {
                channel.writeAndFlush(noData(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error reading reporting data. For user {}. Error: {}", user.email, e.getMessage());
                channel.writeAndFlush(serverError(msgId), channel.voidPromise());
            }
        });
    }

}
