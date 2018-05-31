package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.Holder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static cc.blynk.server.internal.CommonByteBufUtil.energyLimit;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

public class CreateReportLogic {

    private static final Logger log = LogManager.getLogger(CreateReportLogic.class);

    private final int reportsLimit;
    private final ScheduledThreadPoolExecutor reportsExecutor;
    private final MailWrapper mailWrapper;

    public CreateReportLogic(Holder holder) {
        this.reportsLimit = holder.limits.reportsLimit;
        this.reportsExecutor = holder.reportsExecutor;
        this.mailWrapper = holder.mailWrapper;
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

        if (!report.isValid()) {
            log.debug("Report is not valid {} for {}.", report, user.email);
            throw new IllegalCommandException("Report is not valid.");
        }

        if (report.isPeriodic()) {
            log.info("Adding periodic report to scheduler : {} for {}.", report, user.email);

            long initialDelaySeconds = report.calculateDelayInSeconds();

            report.nextReportAt = System.currentTimeMillis() + initialDelaySeconds * 1000;

            if (report.isActive) {
                reportsExecutor.schedule(
                        new ReportTask(user.email, user.appName, report) {
                            @Override
                            public void run() {
                                try {
                                    long now = System.currentTimeMillis();
                                    mailWrapper.sendText(report.recipients, report.name, "Your report is ready.");
                                    log.info("Processed report for  {},time {} ms. Report : {}.",
                                            this.email, System.currentTimeMillis() - now, this.report);
                                    this.report.lastReportAt = now;
                                    long initialDelaySeconds = report.calculateDelayInSeconds();

                                    //rescheduling report
                                    reportsExecutor.schedule(this, initialDelaySeconds, TimeUnit.SECONDS);
                                } catch (IllegalCommandException ice) {
                                    log.debug("Seems like report {} is expired for {}.", report, user.email, ice);
                                } catch (Exception e) {
                                    log.debug("Error generating report {} for {}.", report, user.email, e);
                                } finally {

                                    this.report.lastReportAt = System.currentTimeMillis();
                                }
                            }
                        },
                        initialDelaySeconds,
                        TimeUnit.SECONDS
                );
            }
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }
}
