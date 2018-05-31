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

import java.util.concurrent.ScheduledThreadPoolExecutor;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

public class UpdateReportLogic {

    private static final Logger log = LogManager.getLogger(UpdateReportLogic.class);

    private final ScheduledThreadPoolExecutor reportsExecutor;

    public UpdateReportLogic(Holder holder) {
        this.reportsExecutor = holder.reportsExecutor;
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

        if (report.isValid()) {
            log.info("Updating report in scheduler : {} for {}", report, user.email);
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }
}
