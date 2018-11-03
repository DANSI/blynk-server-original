package cc.blynk.server.application.handlers.main.logic.graph;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.logic.graph.links.DeviceFileLink;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.Superchart;
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
@Deprecated
public final class MobileExportGraphDataLogic {

    private static final Logger log = LogManager.getLogger(MobileExportGraphDataLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final ReportingDiskDao reportingDao;
    private final MailWrapper mailWrapper;
    private final String csvDownloadUrl;

    public MobileExportGraphDataLogic(Holder holder) {
        this.reportingDao = holder.reportingDiskDao;
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.mailWrapper = holder.mailWrapper;
        this.csvDownloadUrl = holder.downloadUrl;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] messageParts = message.body.split(BODY_SEPARATOR_STRING);

        if (messageParts.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        String[] dashIdAndDeviceId = split2Device(messageParts[0]);
        int dashId = Integer.parseInt(dashIdAndDeviceId[0]);
        int targetId = -1;

        if (dashIdAndDeviceId.length == 2) {
            targetId = Integer.parseInt(dashIdAndDeviceId[1]);
        }

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        long widgetId = Long.parseLong(messageParts[1]);

        Widget widget = dash.getWidgetById(widgetId);
        if (widget == null) {
            widget = dash.getWidgetByIdInDeviceTilesOrThrow(widgetId);
        }

        if (widget instanceof Superchart) {
            Superchart enhancedHistoryGraph = (Superchart) widget;

            blockingIOProcessor.executeHistory(
                    new ExportEnhancedHistoryGraphJob(ctx, dash, targetId, enhancedHistoryGraph, message.id, user)
            );
        } else {
            throw new IllegalCommandException("Passed wrong widget id.");
        }
    }

    private class ExportEnhancedHistoryGraphJob implements Runnable {

        private final ChannelHandlerContext ctx;
        private final DashBoard dash;
        private final int targetId;
        private final Superchart enhancedHistoryGraph;
        private final int msgId;
        private final User user;

        ExportEnhancedHistoryGraphJob(ChannelHandlerContext ctx, DashBoard dash, int targetId,
                                      Superchart enhancedHistoryGraph, int msgId, User user) {
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
                String dashName = dash.getNameOrEmpty();
                ArrayList<DeviceFileLink> pinsCSVFilePath = new ArrayList<>();
                for (GraphDataStream graphDataStream : enhancedHistoryGraph.dataStreams) {
                    DataStream dataStream = graphDataStream.dataStream;
                    //special case, for device tiles widget targetID may be overrided
                    int deviceId = graphDataStream.getTargetId(targetId);
                    if (dataStream != null) {
                        try {
                            int[] deviceIds = new int[] {deviceId};
                            //special case, this is not actually a deviceId but device selector widget id
                            //todo refactor/simplify/test
                            if (deviceId >= DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
                                Widget deviceSelector = dash.getWidgetById(deviceId);
                                if (deviceSelector == null) {
                                    deviceSelector = dash.getWidgetByIdInDeviceTilesOrThrow(deviceId);
                                }
                                if (deviceSelector instanceof DeviceSelector) {
                                    deviceIds = ((DeviceSelector) deviceSelector).deviceIds;
                                }
                            }

                            Path path = reportingDao.csvGenerator.createCSV(
                                    user, dash.id, deviceId, dataStream.pinType, dataStream.pin, deviceIds);
                            Device device = user.profile.getDeviceById(dash, deviceId);
                            String name = (device == null || device.name == null) ? dashName : device.name;
                            pinsCSVFilePath.add(new DeviceFileLink(path, name, dataStream.pinType, dataStream.pin));
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
                    String bodyWithLinks = DeviceFileLink.makeBody(csvDownloadUrl, pinsCSVFilePath);
                    mailWrapper.sendHtml(user.email, title, bodyWithLinks);
                    ctx.writeAndFlush(ok(msgId), ctx.voidPromise());
                }

            } catch (Exception e) {
                log.error("Error making csv file for data export. Reason {}", e.getMessage());
                ctx.writeAndFlush(notificationError(msgId), ctx.voidPromise());
            }
        }
    }
}
