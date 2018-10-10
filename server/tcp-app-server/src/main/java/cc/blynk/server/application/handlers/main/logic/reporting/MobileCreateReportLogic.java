package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.Holder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportScheduler;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.CREATE_REPORT;
import static cc.blynk.server.internal.CommonByteBufUtil.energyLimit;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31/05/2018.
 *
 */
public final class MobileCreateReportLogic {

    private static final Logger log = LogManager.getLogger(MobileCreateReportLogic.class);

    private MobileCreateReportLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       User user, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int reportsLimit = holder.limits.reportsLimit;
        ReportScheduler reportScheduler = holder.reportScheduler;

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

        if (reportingWidget.reports.length >= reportsLimit) {
            throw new IllegalCommandException("User reached reports limit.");
        }

        Report report = JsonParser.parseReport(reportJson, message.id);
        reportingWidget.validateId(report.id);

        int price = Report.getPrice();
        if (user.notEnoughEnergy(price)) {
            log.debug("Not enough energy.");
            ctx.writeAndFlush(energyLimit(message.id), ctx.voidPromise());
            return;
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

            if (report.isActive) {
                reportScheduler.schedule(user, dashId, report, initialDelaySeconds);
            }
        }

        user.subtractEnergy(price);
        reportingWidget.reports = ArrayUtil.add(reportingWidget.reports, report, Report.class);
        dash.updatedAt = System.currentTimeMillis();

        ctx.writeAndFlush(makeUTF8StringMessage(CREATE_REPORT, message.id, report.toString()), ctx.voidPromise());
    }

}
