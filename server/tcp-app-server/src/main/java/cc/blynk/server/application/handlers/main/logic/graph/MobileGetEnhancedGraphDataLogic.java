package cc.blynk.server.application.handlers.main.logic.graph;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphPeriod;
import cc.blynk.server.core.model.widgets.outputs.graph.Superchart;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
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
public final class MobileGetEnhancedGraphDataLogic {

    private static final Logger log = LogManager.getLogger(MobileGetEnhancedGraphDataLogic.class);

    private MobileGetEnhancedGraphDataLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       MobileStateHolder state, StringMessage message) {
        String[] messageParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        if (messageParts.length < 3) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int targetId = -1;
        String[] dashIdAndTargetIdString = split2Device(messageParts[0]);
        if (dashIdAndTargetIdString.length == 2) {
            targetId = Integer.parseInt(dashIdAndTargetIdString[1]);
        }
        int dashId = Integer.parseInt(dashIdAndTargetIdString[0]);

        long widgetId = Long.parseLong(messageParts[1]);
        GraphPeriod graphPeriod = GraphPeriod.valueOf(messageParts[2]);
        int page = 0;
        if (messageParts.length == 4) {
            page = Integer.parseInt(messageParts[3]);
        }
        int skipCount = graphPeriod.numberOfPoints * page;

        Profile profile = state.user.profile;
        DashBoard dash = profile.getDashByIdOrThrow(dashId);
        Widget widget = dash.getWidgetById(widgetId);

        //special case for device tiles widget.
        if (widget == null) {
            DeviceTiles deviceTiles = dash.getWidgetByType(DeviceTiles.class);
            if (deviceTiles != null) {
                widget = deviceTiles.getWidgetById(widgetId);
            }
        }

        if (!(widget instanceof Superchart)) {
            throw new IllegalCommandException("Passed wrong widget id.");
        }

        Superchart enhancedHistoryGraph = (Superchart) widget;

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
            Target target;
            int targetIdUpdated = graphDataStream.getTargetId(targetId);
            if (targetIdUpdated < Tag.START_TAG_ID) {
                target = profile.getDeviceById(dash, targetIdUpdated);
            } else if (targetIdUpdated < DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
                target = profile.getTagById(dash, targetIdUpdated);
            } else {
                target = dash.getDeviceSelector(targetIdUpdated);
            }
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
