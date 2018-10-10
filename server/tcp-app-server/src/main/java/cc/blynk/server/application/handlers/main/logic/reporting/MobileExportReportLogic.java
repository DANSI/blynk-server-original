package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.Holder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.ui.reporting.BaseReportTask;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportScheduler;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.QuotaLimitException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

import static cc.blynk.server.core.protocol.enums.Command.EXPORT_REPORT;
import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommand;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31/05/2018.
 *
 */
public final class MobileExportReportLogic {

    private static final Logger log = LogManager.getLogger(MobileExportReportLogic.class);

    private final static long runDelay = TimeUnit.MINUTES.toMillis(1);

    private MobileExportReportLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       User user, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = Integer.parseInt(split[0]);
        int reportId = Integer.parseInt(split[1]);

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        ReportingWidget reportingWidget = dash.getReportingWidget();

        if (reportingWidget == null) {
            throw new IllegalCommandException("Project has no reporting widget.");
        }

        Report report = reportingWidget.getReportById(reportId);
        if (report == null) {
            throw new IllegalCommandException("Cannot find report with passed id.");
        }

        if (!report.isValid()) {
            log.debug("Report is not valid {} for {}.", report, user.email);
            throw new IllegalCommandException("Report is not valid.");
        }

        long now = System.currentTimeMillis();
        if (report.lastReportAt + runDelay > now) {
            log.debug("Report {} trigger limit reached for {}.", report.id, user.email);
            throw new QuotaLimitException("Report trigger limit reached.");
        }

        ReportScheduler reportScheduler = holder.reportScheduler;
        reportScheduler.schedule(new BaseReportTask(user, dashId, report,
                reportScheduler.mailWrapper, reportScheduler.reportingDao,
                reportScheduler.downloadUrl) {
            @Override
            public void run() {
                try {
                    report.lastReportAt = generateReport();
                    if (ctx.channel().isWritable()) {
                        ctx.writeAndFlush(
                                makeUTF8StringMessage(EXPORT_REPORT, message.id, report.toString()),
                                ctx.voidPromise()
                        );
                    }
                } catch (Exception e) {
                    log.debug("Error generating export report {} for {}.", report, key.user.email, e);
                    ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
                }
            }
        }, 0, TimeUnit.SECONDS);
    }
}
