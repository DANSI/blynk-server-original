package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportScheduler;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31/05/2018.
 *
 */
public class UpdateReportLogic {

    private static final Logger log = LogManager.getLogger(UpdateReportLogic.class);

    private final ReportScheduler reportScheduler;
    private final MailWrapper mailWrapper;
    private final ReportingDao reportingDao;

    public UpdateReportLogic(Holder holder) {
        this.reportScheduler = holder.reportScheduler;
        this.mailWrapper = holder.mailWrapper;
        this.reportingDao = holder.reportingDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = Integer.parseInt(split[0]);
        String reportJson = split[1];

        if (reportJson == null || reportJson.isEmpty()) {
            throw new IllegalCommandException("Income report message is empty.");
        }

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        ReportingWidget reportingWidget = dash.getReportingWidget();

        if (reportingWidget == null) {
            throw new IllegalCommandException("Project has no reporting widget.");
        }

        Report report = JsonParser.parseReport(reportJson, message.id);

        int existingReportIndex = reportingWidget.getReportIndexById(report.id);
        if (existingReportIndex == -1) {
            throw new IllegalCommandException("Cannot find report with provided id.");
        }

        reportingWidget.reports = ArrayUtil.copyAndReplace(reportingWidget.reports, report, existingReportIndex);
        dash.updatedAt = System.currentTimeMillis();

        //always remove prev report before any validations are done
        boolean isRemoved = reportScheduler.cancelStoredFuture(new ReportTask(user, dashId, report));
        log.debug("Deleting reportId {} in scheduler for {}. Is removed: {}?.",
                report.id, user.email, isRemoved);

        if (!report.isValid()) {
            log.debug("Report is not valid {} for {}.", report, user.email);
            throw new IllegalCommandException("Report is not valid.");
        }

        if (report.isPeriodic()) {
            long initialDelaySeconds = report.calculateDelayInSeconds();
            log.info("Adding periodic report for user {} with delay {} to scheduler.",
                    user.email, initialDelaySeconds);
            log.debug(reportJson);

            report.nextReportAt = System.currentTimeMillis() + initialDelaySeconds * 1000;

            if (report.isActive) {
                reportScheduler.schedule(
                        new ReportTask(user, dashId, report, reportScheduler, mailWrapper, reportingDao),
                        initialDelaySeconds,
                        TimeUnit.SECONDS
                );
            }
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
