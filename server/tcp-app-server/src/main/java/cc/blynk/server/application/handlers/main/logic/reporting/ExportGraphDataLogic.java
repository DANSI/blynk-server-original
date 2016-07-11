package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.GraphType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.HistoryGraph;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.utils.FileUtils;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.utils.ByteBufUtil.*;
import static java.lang.String.*;

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

    public static final String EXPORT_GRAPH_FILENAME = "%s_%s_%c%d.csv.gz";

    private final BlockingIOProcessor blockingIOProcessor;
    private final ReportingDao reportingDao;
    private final MailWrapper mailWrapper;

    //data for 1 month
    private static final int DEFAULT_FETCH_COUNT = 60 * 24 * 30 * 1;

    private final String tempDir;

    public ExportGraphDataLogic(ReportingDao reportingDao, BlockingIOProcessor blockingIOProcessor, MailWrapper mailWrapper) {
        this.reportingDao = reportingDao;
        this.blockingIOProcessor = blockingIOProcessor;
        this.mailWrapper = mailWrapper;
        this.tempDir = System.getProperty("java.io.tmpdir");
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] messageParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        if (messageParts.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = ParseUtil.parseInt(messageParts[0]);
        long widgetId = ParseUtil.parseLong(messageParts[1]);

        DashBoard dashBoard = user.profile.getDashByIdOrThrow(dashId);

        Widget widget = dashBoard.getWidgetById(widgetId);
        if (!(widget instanceof HistoryGraph)) {
            throw new IllegalCommandException("Passed wrong widget id.");
        }

        HistoryGraph historyGraph = (HistoryGraph) widget;

        blockingIOProcessor.execute(() -> {
            try {
                List<Path> pinsCSVFilePath = new ArrayList<>();
                for (Pin pin : historyGraph.pins) {
                    if (pin != null && pin.pinType != null && pin.pin > -1) {
                        ByteBuffer onePinData = reportingDao.getByteBufferFromDisk(user.name, dashId, pin.pinType, pin.pin, DEFAULT_FETCH_COUNT, GraphType.HOURLY);
                        if (onePinData != null) {
                            String filename = format(EXPORT_GRAPH_FILENAME, user.name, dashId, pin.pinType.pintTypeChar, pin.pin);
                            Path path = Paths.get(tempDir, filename);
                            try {
                                FileUtils.makeGzippedCSVFile(onePinData, path);
                                pinsCSVFilePath.add(path);
                            } catch (Exception e) {
                                log.error("Error making csv file for data export. File {}. Reason {}", filename, e.getMessage());
                            }
                        }
                    }
                }

                if (pinsCSVFilePath.size() == 0) {
                    ctx.writeAndFlush(makeResponse(message.id, NO_DATA_EXCEPTION), ctx.voidPromise());
                } else {
                    String title = "History graph data for project " + (dashBoard.name == null ? "" : dashBoard.name);
                    mailWrapper.send(user.name, title, "", pinsCSVFilePath);
                    ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
                }

            } catch (Exception e) {
                log.error("Error making csv file for data export. Reason {}", e.getMessage());
                ctx.writeAndFlush(makeResponse(message.id, NOTIFICATION_EXCEPTION), ctx.voidPromise());
            }
        });
    }

}
