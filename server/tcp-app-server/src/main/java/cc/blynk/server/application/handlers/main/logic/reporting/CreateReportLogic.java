package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.Holder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.energyLimit;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

public class CreateReportLogic {

    private static final Logger log = LogManager.getLogger(CreateReportLogic.class);

    private final int reportsLimit;

    public CreateReportLogic(Holder holder) {
        this.reportsLimit = holder.limits.reportsLimit;
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

        if (reportingWidget.reports.length >= reportsLimit) {
            throw new IllegalCommandException("User reached reports limit.");
        }

        Report report = JsonParser.parseReport(reportJson, message.id);

        int price = Report.getPrice();
        if (user.notEnoughEnergy(price)) {
            log.debug("Not enough energy.");
            ctx.writeAndFlush(energyLimit(message.id), ctx.voidPromise());
            return;
        }
        user.subtractEnergy(price);

        reportingWidget.reports = ArrayUtil.add(reportingWidget.reports, report, Report.class);
        dash.updatedAt = System.currentTimeMillis();

        if (report.isValid()) {
            log.info("Adding report to scheduler : {} for {}.", report, user.email);
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }
}
