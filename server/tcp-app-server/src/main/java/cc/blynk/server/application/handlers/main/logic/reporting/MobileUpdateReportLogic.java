package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.Holder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportResult;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportScheduler;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.UPDATE_REPORT;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31/05/2018.
 *
 */
public final class MobileUpdateReportLogic {

    private static final Logger log = LogManager.getLogger(MobileUpdateReportLogic.class);

    private MobileUpdateReportLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       User user, StringMessage message) {
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

        ReportScheduler reportScheduler = holder.reportScheduler;

        //always remove prev report before any validations are done
        if (report.isPeriodic()) {
            boolean isRemoved = reportScheduler.cancelStoredFuture(user, dashId, report.id);
            log.debug("Deleting reportId {} in scheduler for {}. Is removed: {}?.",
                    report.id, user.email, isRemoved);
        }

        if (!report.isValid()) {
            log.debug("Report is not valid {} for {}.", report, user.email);
            throw new IllegalCommandException("Report is not valid.");
        }

        if (report.isPeriodic()) {
            long initialDelaySeconds;
            try {
                initialDelaySeconds = report.calculateDelayInSeconds();
            } catch (IllegalCommandBodyException e) {
                //re throw, quick workaround
                log.debug("Report has wrong configuration for {}. Report : {}", user.email, report);
                throw new IllegalCommandBodyException(e.getMessage(), message.id);
            }

            log.info("Adding periodic report for user {} with delay {} to scheduler.",
                    user.email, initialDelaySeconds);
            log.debug(reportJson);

            report.nextReportAt = System.currentTimeMillis() + initialDelaySeconds * 1000;
            //special case when expired report is extended
            if (report.lastRunResult == ReportResult.EXPIRED) {
                report.lastRunResult = null;
            }

            if (report.isActive) {
                reportScheduler.schedule(user, dashId, report, initialDelaySeconds);
            }
        }

        reportingWidget.reports = ArrayUtil.copyAndReplace(reportingWidget.reports, report, existingReportIndex);
        dash.updatedAt = System.currentTimeMillis();

        ctx.writeAndFlush(makeUTF8StringMessage(UPDATE_REPORT, message.id, report.toString()), ctx.voidPromise());
    }

}
