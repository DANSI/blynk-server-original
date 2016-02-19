package cc.blynk.server.db;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

/**
 * Creates and holds prepared statements for reporting queries.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.02.16.
 */
public class ReportingQueries {

    //max minute graph granularity is 6 hours
    private static final int MINUTE_RECORD_TTL = 6 * 60 * 60;
    //max hour graph granularity is 7 days
    private static final int HOUR_RECORD_TTL = 7 * 24 * 60 * 60;
    //max month graph granularity 1 year
    private static final int MONTH_RECORD_TTL = 365 * 24 * 60 * 60;
    private static Logger log = LogManager.getLogger(ReportingQueries.class);
    public final PreparedStatement insertIntoReportingMinute;
    public final PreparedStatement insertIntoReportingHourly;
    public final PreparedStatement insertIntoReportingDaily;

    public final PreparedStatement selectFromReportingMinute;
    public final PreparedStatement selectFromReportingHour;
    public final PreparedStatement selectFromReportingDaily;

    public ReportingQueries(String reportingKeyspace, Session session) {
        Insert insertMinute = makeReportingInsert(reportingKeyspace, "average_minute", MINUTE_RECORD_TTL);
        log.info("Prepared minute report query : {}", insertMinute.toString());
        insertIntoReportingMinute = session.prepare(insertMinute);

        Insert insertHour = makeReportingInsert(reportingKeyspace, "average_hourly", HOUR_RECORD_TTL);
        log.info("Prepared hour report query : {}", insertHour.toString());
        insertIntoReportingHourly = session.prepare(insertHour);

        Insert insertDay = makeReportingInsert(reportingKeyspace, "average_daily", MONTH_RECORD_TTL);
        log.info("Prepared day report query : {}", insertDay.toString());
        insertIntoReportingDaily = session.prepare(insertDay);



        Select selectMinute = makeReportingSelect(reportingKeyspace, "average_minute");
        log.info("Prepared minute select : {} ", selectMinute.toString());
        selectFromReportingMinute = session.prepare(selectMinute);

        Select selectHour = makeReportingSelect(reportingKeyspace, "average_hourly");
        log.info("Prepared hour select : {} ", selectHour.toString());
        selectFromReportingHour = session.prepare(selectHour);

        Select selectDay = makeReportingSelect(reportingKeyspace, "average_daily");
        log.info("Prepared day select : {} ", selectDay.toString());
        selectFromReportingDaily = session.prepare(selectDay);
    }

    private static Select makeReportingSelect(String reportingKeyspace, String tableName) {
        Select selectReporting = QueryBuilder.select("ts", "value")
                .from(reportingKeyspace, tableName);
        selectReporting.where(eq("username", bindMarker()));
        selectReporting.where(eq("project_id", bindMarker()));
        selectReporting.where(eq("pin", bindMarker()));
        selectReporting.where(eq("pinType", bindMarker()));
        selectReporting.limit(bindMarker());

        return selectReporting;
    }

    private static Insert makeReportingInsert(String reportingKeyspace, String tableName) {
        return QueryBuilder.insertInto(reportingKeyspace, tableName)
                .value("username", bindMarker())
                .value("project_id",  bindMarker())
                .value("pin",  bindMarker())
                .value("pinType",  bindMarker())
                .value("ts",  bindMarker())
                .value("value", bindMarker());
    }

    private static Insert makeReportingInsert(String reportingKeyspace, String tableName, int ttl) {
        Insert insert = makeReportingInsert(reportingKeyspace, tableName);
        insert.using(ttl(ttl));
        return insert;
    }
}
