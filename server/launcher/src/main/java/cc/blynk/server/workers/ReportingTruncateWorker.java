package cc.blynk.server.workers;

import cc.blynk.server.core.dao.CSVGenerator;
import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static cc.blynk.utils.ReportingUtil.REPORTING_RECORD_SIZE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.01.18.
 */
public class ReportingTruncateWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(ReportingTruncateWorker.class);

    private final ReportingDiskDao reportingDao;
    private final long exportExpirePeriod;
    private final int maxRecordsCount;

    public ReportingTruncateWorker(ReportingDiskDao reportingDao, int storeMinuteRecordDays, long storeReportCSVDays) {
        //storing minute points only for 10 days
        this.reportingDao = reportingDao;
        this.maxRecordsCount = (int) TimeUnit.DAYS.toMinutes(storeMinuteRecordDays);
        this.exportExpirePeriod = TimeUnit.DAYS.toMillis(storeReportCSVDays);
    }

    @Override
    public void run() {
        long now;

        try {
            now = System.currentTimeMillis();
            int result = truncateOutdatedData();
            log.info("Truncated {} files. Time : {} ms.", result, System.currentTimeMillis() - now);
        } catch (Throwable t) {
            log.error("Error truncating unused reporting data.", t);
        }

        try {
            now = System.currentTimeMillis();
            int result = deleteOldExportCsvFiles();
            log.info("Removed {} old export files. Time : {} ms.", result, System.currentTimeMillis() - now);
        } catch (Throwable t) {
            log.error("Error deleting outdated export files.", t);
        }
    }

    private int deleteOldExportCsvFiles() throws IOException {
        long now = System.currentTimeMillis();
        int counter = 0;
        try (DirectoryStream<Path> csvFolder = Files.newDirectoryStream(Paths.get(FileUtils.CSV_DIR), "*")) {
            for (Path csvFile : csvFolder) {
                if (csvFile.getFileName().toString().endsWith(CSVGenerator.EXPORT_CSV_EXTENSION)
                        && isOutdated(csvFile, now)) {
                    counter++;
                    Files.delete(csvFile);
                }
            }
        }
        return counter;
    }

    private boolean isOutdated(Path filePath, long now)  throws IOException {
        long lastModified = FileUtils.getLastModified(filePath);
        return lastModified + exportExpirePeriod < now;
    }

    private int truncateOutdatedData() throws Exception {
        int truncatedFilesCounter = 0;

        Path reportingFolderPath = Paths.get(reportingDao.dataFolder);
        if (Files.notExists(reportingFolderPath)) {
            return 0;
        }

        try (DirectoryStream<Path> reportingFolder = Files.newDirectoryStream(reportingFolderPath, "*")) {
            for (Path userReportingDirectory : reportingFolder) {
                if (Files.isDirectory(userReportingDirectory)) {
                    int filesCounter = 0;
                    try {
                        try (DirectoryStream<Path> userReportingFolder = directoryStream(userReportingDirectory)) {
                            for (Path userReportingFile : userReportingFolder) {
                                filesCounter++;
                                long fileSize = Files.size(userReportingFile);
                                if (fileSize > maxRecordsCount * REPORTING_RECORD_SIZE) {
                                    ByteBuffer userReportingData = FileUtils.read(userReportingFile, maxRecordsCount);
                                    try (OutputStream os =
                                                 Files.newOutputStream(userReportingFile, TRUNCATE_EXISTING)) {
                                        os.write(userReportingData.array());
                                    }
                                    truncatedFilesCounter++;
                                }
                            }
                        }
                        if (filesCounter == 0) {
                            Files.delete(userReportingDirectory);
                        }
                    } catch (Exception e) {
                        log.error("Truncation failed for {}. Reason : {}.", userReportingDirectory, e.getMessage());
                    }
                }
            }
        }
        return truncatedFilesCounter;
    }

    private static final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*_minute.bin");
    private static final DirectoryStream.Filter<Path> filter = entry -> matcher.matches(entry.getFileName());

    //utility method to avoid allocation of PathMatcher
    private DirectoryStream<Path> directoryStream(Path dir) throws IOException {
        // create a matcher and return a filter that uses it.
        return dir.getFileSystem().provider().newDirectoryStream(dir, filter);
    }
}
