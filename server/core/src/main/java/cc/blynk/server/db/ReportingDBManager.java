package cc.blynk.server.db;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.reporting.average.AggregationKey;
import cc.blynk.server.core.reporting.average.AggregationValue;
import cc.blynk.server.core.stats.model.Stat;
import cc.blynk.server.db.dao.ReportingDBDao;
import cc.blynk.utils.properties.BaseProperties;
import cc.blynk.utils.properties.DBProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;

import static cc.blynk.utils.properties.DBProperties.DB_PROPERTIES_FILENAME;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.02.16.
 */
public class ReportingDBManager implements Closeable {

    private static final Logger log = LogManager.getLogger(ReportingDBManager.class);
    private final HikariDataSource ds;

    private final BlockingIOProcessor blockingIOProcessor;
    private final boolean cleanOldReporting;

    public ReportingDBDao reportingDBDao;

    public ReportingDBManager(BlockingIOProcessor blockingIOProcessor, boolean isEnabled) {
        this(DB_PROPERTIES_FILENAME, blockingIOProcessor, isEnabled);
    }

    public ReportingDBManager(String propsFilename, BlockingIOProcessor blockingIOProcessor, boolean isEnabled) {
        this.blockingIOProcessor = blockingIOProcessor;

        DBProperties dbProperties = new DBProperties(propsFilename);
        if (!isEnabled || dbProperties.size() == 0) {
            log.info("Separate DB storage disabled.");
            this.ds = null;
            this.cleanOldReporting = false;
            return;
        }

        HikariConfig config = initConfig(dbProperties);

        log.info("Reporting DB url : {}", config.getJdbcUrl());
        log.info("Reporting DB user : {}", config.getUsername());
        log.info("Connecting to reporting DB...");

        HikariDataSource hikariDataSource;
        try {
            hikariDataSource = new HikariDataSource(config);
        } catch (Exception e) {
            log.error("Not able connect to reporting DB. Skipping. Reason : {}", e.getMessage());
            this.ds = null;
            this.cleanOldReporting = false;
            return;
        }

        this.ds = hikariDataSource;
        this.reportingDBDao = new ReportingDBDao(hikariDataSource);
        this.cleanOldReporting = dbProperties.cleanReporting();

        log.info("Connected to reporting database successfully.");
    }

    private HikariConfig initConfig(BaseProperties serverProperties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(serverProperties.getProperty("reporting.jdbc.url"));
        config.setUsername(serverProperties.getProperty("reporting.user"));
        config.setPassword(serverProperties.getProperty("reporting.password"));

        config.setAutoCommit(false);
        config.setConnectionTimeout(serverProperties.getLongProperty("reporting.connection.timeout.millis"));
        config.setMaximumPoolSize(5);
        config.setMaxLifetime(0);
        config.setConnectionTestQuery("SELECT 1");
        return config;
    }

    public void insertStat(String region, Stat stat) {
        if (isDBEnabled()) {
            reportingDBDao.insertStat(region, stat);
        }
    }

    public void insertReporting(Map<AggregationKey, AggregationValue> map, GraphGranularityType graphGranularityType) {
        if (isDBEnabled() && map.size() > 0) {
            blockingIOProcessor.executeReportingDB(() -> reportingDBDao.insert(map, graphGranularityType));
        }
    }

    public void insertReportingRaw(Map<AggregationKey, Object> rawData) {
        if (isDBEnabled() && rawData.size() > 0) {
            blockingIOProcessor.executeReportingDB(() -> reportingDBDao.insertRawData(rawData));
        }
    }

    public void cleanOldReportingRecords(Instant now) {
        if (isDBEnabled() && cleanOldReporting) {
            blockingIOProcessor.executeReportingDB(() -> reportingDBDao.cleanOldReportingRecords(now));
        }
    }

    public boolean isDBEnabled() {
        return !(ds == null || ds.isClosed());
    }

    public void executeSQL(String sql) throws Exception {
        try (Connection connection = ds.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            connection.commit();
        }
    }

    public Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    @Override
    public void close() {
        if (isDBEnabled()) {
            System.out.println("Closing Reporting DB...");
            ds.close();
        }
    }

}
