package cc.blynk.server.core.model.widgets.ui.reporting;

import cc.blynk.server.core.dao.ReportingStorageDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportDataStream;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31/05/2018.
 *
 */
public abstract class BaseReportTask implements Runnable {

    static final Logger log = LogManager.getLogger(BaseReportTask.class);

    public final ReportTaskKey key;

    final Report report;

    private final MailWrapper mailWrapper;

    private final ReportingStorageDao reportingStorageDao;

    private final String downloadUrl;

    protected BaseReportTask(User user, int dashId, Report report,
                             MailWrapper mailWrapper, ReportingStorageDao reportingStorageDao,
                             String downloadUrl) {
        this.key = new ReportTaskKey(user, dashId, report.id);
        this.report = report;
        this.mailWrapper = mailWrapper;
        this.reportingStorageDao = reportingStorageDao;
        this.downloadUrl = downloadUrl;
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
            report.lastReportAt = generateReport();
            log.debug(report);
        } catch (Exception e) {
            log.debug("Error generating report {} for {}.", report, key.user.email, e);
        }
    }

    protected long generateReport() {
        long now = System.currentTimeMillis();

        String date = LocalDate.now(report.tzName).toString();
        Path userCsvFolder = FileUtils.getUserReportDir(
                key.user.email, key.user.appName, key.reportId, date);

        try {
            report.lastRunResult = generateReport(userCsvFolder);
        } catch (Exception e) {
            report.lastRunResult = ReportResult.ERROR;
            log.error("Error generating report for user {}. ", key.user.email);
            log.error(e);
        }

        long newNow = System.currentTimeMillis();

        log.info("Processed report for {}, time {} ms.", key.user.email, newNow - now);
        return newNow;
    }

    private ReportResult generateReport(Path userCsvFolder) throws Exception {
        int fetchCount = (int) report.reportType.getFetchCount(report.granularityType);
        Path output = Paths.get(userCsvFolder.toString() + ".gz");

        //todo for now supporting only 1 type of output format
        switch (report.reportOutput) {
            case MERGED_CSV:
            case EXCEL_TAB_PER_DEVICE:
            case CSV_FILE_PER_DEVICE:
            case CSV_FILE_PER_DEVICE_PER_PIN:
            default:
                if (filePerDevicePerPin(output, fetchCount)) {
                    ReportFileLink fileLink = new ReportFileLink(output, report.name);
                    String durationLabel = report.reportType.getDurationLabel().toLowerCase();
                    String subj = "Your " + durationLabel + " " + report.name + " is ready";
                    String gzipDownloadUrl = fileLink.makeBody(downloadUrl);
                    String dynamicSection = report.buildDynamicSection();
                    mailWrapper.sendReportEmail(report.recipients, subj, gzipDownloadUrl, dynamicSection);
                    return ReportResult.OK;
                } else {
                    log.info("No data for report for user {} and reportId {}.", key.user, report.id);
                    return ReportResult.NO_DATA;
                }
        }
    }

    private boolean filePerDevicePerPin(Path output, int fetchCount) throws Exception {
        boolean atLeastOne = false;
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(output))) {
            for (ReportSource reportSource : report.reportSources) {
                if (reportSource.isValid()) {
                    for (int deviceId : reportSource.getDeviceIds()) {
                        for (ReportDataStream reportDataStream : reportSource.reportDataStreams) {
                            if (reportDataStream.isValid()) {
                                byte[] onePinDataCsv = processSingleFile(deviceId, reportDataStream, fetchCount);
                                if (onePinDataCsv != null) {
                                    String onePinFileName =
                                            deviceAndPinFileName(key.dashId, deviceId, reportDataStream);
                                    ZipEntry zipEntry = new ZipEntry(onePinFileName);
                                    try {
                                        zs.putNextEntry(zipEntry);
                                        zs.write(onePinDataCsv, 0, onePinDataCsv.length);
                                        zs.closeEntry();
                                        atLeastOne = true;
                                    } catch (IOException e) {
                                        log.error("Error compressing report file.", e.getMessage());
                                        log.debug(e);
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return atLeastOne;
    }

    private byte[] processSingleFile(int deviceId, ReportDataStream reportDataStream, int fetchCount) {
        ByteBuffer onePinData = reportingStorageDao.getByteBufferFromDisk(key.user,
                key.dashId, deviceId, reportDataStream.pinType,
                reportDataStream.pin, fetchCount, report.granularityType, 0);
        if (onePinData != null) {
            ((Buffer) onePinData).flip();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(onePinData.capacity());
            FileUtils.writeBufToCsv(byteArrayOutputStream, onePinData, deviceId);
            return byteArrayOutputStream.toByteArray();
        }
        return null;
    }

}
