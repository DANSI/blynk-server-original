package cc.blynk.server.workers;

import cc.blynk.server.core.dao.ReportingDao;
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

import static cc.blynk.server.internal.ReportingUtil.REPORTING_RECORD_SIZE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.01.18.
 */
public class ReportingTruncateWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(ReportingTruncateWorker.class);

    private final ReportingDao reportingDao;
    //storing minute points only for 30 days
    private final static int MAX_RECORD_COUNT = 30 * 24 * 60;

    public ReportingTruncateWorker(ReportingDao reportingDao) {
        this.reportingDao = reportingDao;
    }

    @Override
    public void run() {
        try {
            log.info("Start truncate unused reporting data...");
            long now = System.currentTimeMillis();

            int result = truncateOutdatedData();
            log.info("Truncated {} files. Time : {} ms.", result, System.currentTimeMillis() - now);
        } catch (Throwable t) {
            log.error("Error truncating unused reporting data.", t);
        }
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
                                if (fileSize > MAX_RECORD_COUNT * REPORTING_RECORD_SIZE) {
                                    ByteBuffer userReportingData = FileUtils.read(userReportingFile, MAX_RECORD_COUNT);
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
                    } catch (Exception e){
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
