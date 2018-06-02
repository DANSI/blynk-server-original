package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.application.handlers.main.logic.graph.links.ReportFileLink;
import cc.blynk.server.core.dao.ReportingStorageDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportScheduler;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportDataStream;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31/05/2018.
 *
 */
public class ReportTask implements Runnable {

    private static final Logger log = LogManager.getLogger(ReportTask.class);

    private final User user;

    private final int dashId;

    private final int reportId;

    private final Report report;

    private final ReportScheduler reportScheduler;

    private final MailWrapper mailWrapper;

    private final ReportingStorageDao reportingDao;

    ReportTask(User user, int dashId, Report report,
               ReportScheduler reportScheduler, MailWrapper mailWrapper, ReportingStorageDao reportingDao) {
        this.user = user;
        this.dashId = dashId;
        this.reportId = report.id;
        this.report = report;
        this.reportScheduler = reportScheduler;
        this.mailWrapper = mailWrapper;
        this.reportingDao = reportingDao;
    }

    ReportTask(User user, int dashId, Report report) {
        this(user, dashId, report, null, null, null);
    }

    private static String deviceAndPinFileName(int dashId, int deviceId, ReportDataStream reportDataStream) {
        return deviceAndPinFileName(dashId, deviceId, reportDataStream.pinType, reportDataStream.pin);
    }

    private static String deviceAndPinFileName(int dashId, int deviceId, PinType pinType, byte pin) {
        return dashId + "_" + deviceId + "_" + pinType.pintTypeChar + pin + ".csv";
    }

    @Override
    public void run() {
        try {
            long now = System.currentTimeMillis();

            String date = LocalDate.now(report.tzName).toString();
            Path userCsvFolder = FileUtils.getUserReportDir(user.email, user.appName, reportId, date);
            if (Files.notExists(userCsvFolder)) {
                Files.createDirectories(userCsvFolder);
            }

            generateReport(userCsvFolder);

            long newNow = System.currentTimeMillis();

            log.info("Processed report for {}, time {} ms.", user.email, newNow - now);
            log.debug(report);

            report.lastReportAt = newNow;
            long initialDelaySeconds = report.calculateDelayInSeconds();
            report.nextReportAt = newNow + initialDelaySeconds * 1000;

            //rescheduling report
            log.info("Rescheduling report for {} with delay {}.",
                    user.email, initialDelaySeconds);
            reportScheduler.schedule(this, initialDelaySeconds, TimeUnit.SECONDS);
        } catch (IllegalCommandException ice) {
            log.info("Seems like report is expired for {}.", user.email);
            report.nextReportAt = -1L;
        } catch (Exception e) {
            log.debug("Error generating report {} for {}.", report, user.email, e);
        }
    }

    private void generateReport(Path userCsvFolder) {
        int fetchCount = (int) report.reportType.getFetchCount(report.granularityType);
        //todo for now supporting only 1 type of output format
        try {
            switch (report.reportOutput) {
                case MERGED_CSV:
                case EXCEL_TAB_PER_DEVICE:
                case CSV_FILE_PER_DEVICE:
                case CSV_FILE_PER_DEVICE_PER_PIN:
                default:
                    if (filePerDevicePerPin(userCsvFolder, fetchCount)) {
                        Path gzippedResult = gzipFolder(userCsvFolder);
                        ReportFileLink fileLink = new ReportFileLink(gzippedResult, report.name);
                        String reportSubj = "Your report " + report.name + " is ready!";
                        String reportBody = fileLink.makeBody(reportScheduler.downloadUrl);
                        mailWrapper.sendHtml(report.recipients, reportSubj, reportBody);
                    }
                    break;
            }

        } catch (Exception e) {
            log.error("Error generating report for user {}. ", user.email);
            log.error(e);
        }
    }

    private boolean filePerDevicePerPin(Path userCsvFolder, int fetchCount) {
        boolean atLeastOneFile = false;
        for (ReportSource reportSource : report.reportSources) {
            if (reportSource.isValid()) {
                for (int deviceId : reportSource.getDeviceIds()) {
                    for (ReportDataStream reportDataStream : reportSource.reportDataStreams) {
                        if (reportDataStream.isValid()) {
                            //todo gzipping may be done on the fly
                            atLeastOneFile = processSingleFile(userCsvFolder, deviceId, reportDataStream, fetchCount);
                        }
                    }
                }
            }
        }
        return atLeastOneFile;
    }

    private Path gzipFolder(Path userCsvFolder) throws IOException {
        Path output = Paths.get(userCsvFolder.toString() + ".gz");
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(output))) {
            Files.walk(userCsvFolder)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(userCsvFolder.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            log.error("Error compressing report file.", e.getMessage());
                            log.debug(e);
                            throw new RuntimeException(e);
                        }
                    });
        }

        return output;
    }

    private boolean processSingleFile(Path userCsvFolder, int deviceId,
                                      ReportDataStream reportDataStream, int fetchCount) {
        boolean atLeastOneFile = false;
        try {
            ByteBuffer onePinData =
                    reportingDao.getByteBufferFromDisk(user,
                            dashId, deviceId, reportDataStream.pinType,
                            reportDataStream.pin, fetchCount, report.granularityType, 0);
            if (onePinData != null) {
                ((Buffer) onePinData).flip();
                Path onePinFileName = Paths.get(userCsvFolder.toString(),
                        deviceAndPinFileName(dashId, deviceId, reportDataStream));
                try (BufferedWriter bufferedWriter = Files.newBufferedWriter(onePinFileName)) {
                    FileUtils.writeBufToCsv(bufferedWriter, onePinData, deviceId);
                }
                atLeastOneFile = true;
            }
        } catch (Exception e) {
            log.error("Error generating single file for report for {}. Reason : {}", userCsvFolder, e.getMessage());
        }
        return atLeastOneFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReportTask that = (ReportTask) o;
        return dashId == that.dashId
                && reportId == that.reportId
                && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, dashId, reportId);
    }
}
