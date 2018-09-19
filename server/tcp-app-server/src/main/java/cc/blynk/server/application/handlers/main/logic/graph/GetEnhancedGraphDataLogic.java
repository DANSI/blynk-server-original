package cc.blynk.server.application.handlers.main.logic.graph;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphPeriod;
import cc.blynk.server.core.model.widgets.outputs.graph.Superchart;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
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
import static cc.blynk.server.internal.CommonByteBufUtil.makeBinaryMessage;
import static cc.blynk.server.internal.CommonByteBufUtil.noData;
import static cc.blynk.server.internal.CommonByteBufUtil.serverError;
import static cc.blynk.utils.ByteUtils.compress;
import static cc.blynk.utils.StringUtils.split2Device;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class GetEnhancedGraphDataLogic {

    private static final Logger log = LogManager.getLogger(GetEnhancedGraphDataLogic.class);

    private GetEnhancedGraphDataLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       AppStateHolder state, StringMessage message) {
        var messageParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        if (messageParts.length < 3) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int targetId = -1;
        var dashIdAndTargetIdString = split2Device(messageParts[0]);
        if (dashIdAndTargetIdString.length == 2) {
            targetId = Integer.parseInt(dashIdAndTargetIdString[1]);
        }
        var dashId = Integer.parseInt(dashIdAndTargetIdString[0]);

        var widgetId = Long.parseLong(messageParts[1]);
        var graphPeriod = GraphPeriod.valueOf(messageParts[2]);
        var page = 0;
        if (messageParts.length == 4) {
            page = Integer.parseInt(messageParts[3]);
        }
        var skipCount = graphPeriod.numberOfPoints * page;

        var dash = state.user.profile.getDashByIdOrThrow(dashId);
        var widget = dash.getWidgetById(widgetId);

        //special case for device tiles widget.
        if (widget == null) {
            var deviceTiles = dash.getWidgetByType(DeviceTiles.class);
            if (deviceTiles != null) {
                widget = deviceTiles.getWidgetById(widgetId);
            }
        }

        if (!(widget instanceof Superchart)) {
            throw new IllegalCommandException("Passed wrong widget id.");
        }

        var enhancedHistoryGraph = (Superchart) widget;

        var numberOfStreams = enhancedHistoryGraph.dataStreams.length;
        if (numberOfStreams == 0) {
            log.debug("No data streams for enhanced graph with id {}.", widgetId);
            ctx.writeAndFlush(noData(message.id), ctx.voidPromise());
            return;
        }

        var requestedPins = new GraphPinRequest[enhancedHistoryGraph.dataStreams.length];

        var i = 0;
        for (var graphDataStream : enhancedHistoryGraph.dataStreams) {
            //special case, for device tiles widget targetID may be overrided
            var target = dash.getTarget(graphDataStream.getTargetId(targetId));
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

        readGraphData(holder, ctx.channel(), state.user, requestedPins, message.id);
    }

    private static void readGraphData(Holder holder, Channel channel, User user,
                                      GraphPinRequest[] requestedPins, int msgId) {
        holder.blockingIOProcessor.executeHistory(() -> {
            try {
                byte[][] data = holder.reportingDiskDao.getReportingData(user, requestedPins);
                byte[] compressed = compress(requestedPins[0].dashId, data);

                if (channel.isWritable()) {
                    channel.writeAndFlush(
                            makeBinaryMessage(GET_ENHANCED_GRAPH_DATA, msgId, compressed),
                            channel.voidPromise()
                    );
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
