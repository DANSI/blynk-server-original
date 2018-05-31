package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.Holder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
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

public class DeleteReportLogic {

    private static final Logger log = LogManager.getLogger(DeleteReportLogic.class);

    private final ScheduledThreadPoolExecutor reportsExecutor;

    public DeleteReportLogic(Holder holder) {
        this.reportsExecutor = holder.reportsExecutor;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
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

        int existingReportIndex = reportingWidget.getReportIndexById(reportId);
        if (existingReportIndex == -1) {
            throw new IllegalCommandException("Cannot find report with provided id.");
        }

        Report reportToRemove = reportingWidget.reports[existingReportIndex];
        user.addEnergy(Report.getPrice());
        reportingWidget.reports = ArrayUtil.remove(reportingWidget.reports, existingReportIndex, Report.class);
        dash.updatedAt = System.currentTimeMillis();

        reportsExecutor.remove(new ReportTask(user.email, user.appName, reportToRemove));

        log.info("Deleting report {} in scheduler for {}", reportId, user.email);

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }
}
