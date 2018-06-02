package cc.blynk.server.application.handlers.main.logic.graph;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingStorageDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.HistoryGraph;
import cc.blynk.server.core.model.widgets.outputs.graph.EnhancedHistoryGraph;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.notifications.mail.MailWrapper;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;

import static cc.blynk.server.internal.CommonByteBufUtil.noData;
import static cc.blynk.server.internal.CommonByteBufUtil.notificationError;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR_STRING;
import static cc.blynk.utils.StringUtils.split2Device;

/**
 * Sends graph pins data in csv format via to user email.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class ExportGraphDataLogic {

    private static final Logger log = LogManager.getLogger(ExportGraphDataLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final ReportingStorageDao reportingDao;
    private final MailWrapper mailWrapper;
    private final String csvDownloadUrl;

    public ExportGraphDataLogic(Holder holder) {
        this.reportingDao = holder.reportingDao;
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.mailWrapper = holder.mailWrapper;
        this.csvDownloadUrl = holder.csvDownloadUrl;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        var messageParts = message.body.split(BODY_SEPARATOR_STRING);

        if (messageParts.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        var dashIdAndDeviceId = split2Device(messageParts[0]);
        var dashId = Integer.parseInt(dashIdAndDeviceId[0]);
        var targetId = -1;

        if (dashIdAndDeviceId.length == 2) {
            targetId = Integer.parseInt(dashIdAndDeviceId[1]);
        }

        var dash = user.profile.getDashByIdOrThrow(dashId);

        var widgetId = Long.parseLong(messageParts[1]);

        var widget = dash.getWidgetById(widgetId);
        if (widget == null) {
            widget = dash.getWidgetByIdInDeviceTilesOrThrow(widgetId);
        }

        if (widget instanceof HistoryGraph) {
            var historyGraph = (HistoryGraph) widget;

            blockingIOProcessor.executeHistory(
                    new ExportHistoryGraphJob(ctx, dash, historyGraph, message.id, user)
            );
        } else if (widget instanceof EnhancedHistoryGraph) {
            var enhancedHistoryGraph = (EnhancedHistoryGraph) widget;

            blockingIOProcessor.executeHistory(
                    new ExportEnhancedHistoryGraphJob(ctx, dash, targetId, enhancedHistoryGraph, message.id, user)
            );
        } else {
            throw new IllegalCommandException("Passed wrong widget id.");
        }
    }

    private class ExportHistoryGraphJob implements Runnable {

        private final ChannelHandlerContext ctx;
        private final DashBoard dash;
        private final HistoryGraph historyGraph;
        private final int msgId;
        private final User user;

        ExportHistoryGraphJob(ChannelHandlerContext ctx, DashBoard dash,
                                     HistoryGraph historyGraph, int msgId, User user) {
            this.ctx = ctx;
            this.dash = dash;
            this.historyGraph = historyGraph;
            this.msgId = msgId;
            this.user = user;
        }

        @Override
        public void run() {
            try {
                var dashName = dash.getNameOrEmpty();
                var pinsCSVFilePath = new ArrayList<FileLink>();
                var deviceId = historyGraph.deviceId;
                for (var dataStream : historyGraph.dataStreams) {
                    if (dataStream != null) {
                        try {
                            var deviceIds = new int[] {deviceId};
                            //special case, this is not actually a deviceId but device selector widget id
                            if (deviceId >= DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
                                var deviceSelector = dash.getWidgetById(deviceId);
                                if (deviceSelector instanceof DeviceSelector) {
                                    deviceIds = ((DeviceSelector) deviceSelector).deviceIds;
                                }
                            }
                            var path = reportingDao.csvGenerator.createCSV(
                                    user, dash.id, deviceId, dataStream.pinType, dataStream.pin, deviceIds);
                            pinsCSVFilePath.add(
                                    new FileLink(path.getFileName(), dashName, dataStream.pinType, dataStream.pin));
                        } catch (Exception e) {
                            //ignore eny exception.
                        }
                    }
                }

                if (pinsCSVFilePath.size() == 0) {
                    ctx.writeAndFlush(noData(msgId), ctx.voidPromise());
                } else {
                    var title = "History graph data for project " + dashName;
                    mailWrapper.sendHtml(user.email, title, makeBody(pinsCSVFilePath));
                    ctx.writeAndFlush(ok(msgId), ctx.voidPromise());
                }

            } catch (Exception e) {
                log.error("Error making csv file for data export. Reason {}", e.getMessage());
                if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                    ctx.writeAndFlush(notificationError(msgId), ctx.voidPromise());
                }
            }
        }
    }

    private class ExportEnhancedHistoryGraphJob implements Runnable {

        private final ChannelHandlerContext ctx;
        private final DashBoard dash;
        private final int targetId;
        private final EnhancedHistoryGraph enhancedHistoryGraph;
        private final int msgId;
        private final User user;

        ExportEnhancedHistoryGraphJob(ChannelHandlerContext ctx, DashBoard dash, int targetId,
                              EnhancedHistoryGraph enhancedHistoryGraph, int msgId, User user) {
            this.ctx = ctx;
            this.dash = dash;
            this.targetId = targetId;
            this.enhancedHistoryGraph = enhancedHistoryGraph;
            this.msgId = msgId;
            this.user = user;
        }

        @Override
        public void run() {
            try {
                var dashName = dash.getNameOrEmpty();
                var pinsCSVFilePath = new ArrayList<FileLink>();
                for (GraphDataStream graphDataStream : enhancedHistoryGraph.dataStreams) {
                    DataStream dataStream = graphDataStream.dataStream;
                    //special case, for device tiles widget targetID may be overrided
                    var deviceId = graphDataStream.getTargetId(targetId);
                    if (dataStream != null) {
                        try {
                            var deviceIds = new int[] {deviceId};
                            //special case, this is not actually a deviceId but device selector widget id
                            //todo refactor/simplify/test
                            if (deviceId >= DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
                                long targetId = deviceId;
                                Widget deviceSelector = dash.getWidgetById(targetId);
                                if (deviceSelector == null) {
                                    deviceSelector = dash.getWidgetByIdInDeviceTilesOrThrow(targetId);
                                }
                                if (deviceSelector instanceof DeviceSelector) {
                                    deviceIds = ((DeviceSelector) deviceSelector).deviceIds;
                                }
                            }

                            var path = reportingDao.csvGenerator.createCSV(
                                    user, dash.id, deviceId, dataStream.pinType, dataStream.pin, deviceIds);
                            pinsCSVFilePath.add(
                                    new FileLink(path.getFileName(), dashName, dataStream.pinType, dataStream.pin));
                        } catch (Exception e) {
                            log.debug("Error generating csv file.", e);
                            //ignore any exception.
                        }
                    }
                }

                if (pinsCSVFilePath.size() == 0) {
                    ctx.writeAndFlush(noData(msgId), ctx.voidPromise());
                } else {
                    String title = "History graph data for project " + dashName;
                    mailWrapper.sendHtml(user.email, title, makeBody(pinsCSVFilePath));
                    ctx.writeAndFlush(ok(msgId), ctx.voidPromise());
                }

            } catch (Exception e) {
                log.error("Error making csv file for data export. Reason {}", e.getMessage());
                ctx.writeAndFlush(notificationError(msgId), ctx.voidPromise());
            }
        }
    }

    private static String makeBody(ArrayList<FileLink> fileUrls) {
        var sb = new StringBuilder();
        sb.append("<html><body>");
        for (FileLink link : fileUrls) {
            sb.append(link.toString()).append("<br>");
        }
        return sb.append("</body></html>").toString();
    }

    private class FileLink {
        final Path path;
        final String dashName;
        final PinType pinType;
        final byte pin;

        FileLink(Path path, String dashName, PinType pinType, byte pin) {
            this.path = path;
            this.dashName = dashName;
            this.pinType = pinType;
            this.pin = pin;
        }

        @Override
        public String toString() {
            return "<a href=\"" + csvDownloadUrl + path + "\">" + dashName + " " + pinType.pintTypeChar + pin + "</a>";
        }
    }

}
