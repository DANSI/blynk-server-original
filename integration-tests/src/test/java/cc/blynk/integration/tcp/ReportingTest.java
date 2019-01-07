package cc.blynk.integration.tcp;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.ui.reporting.Format;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportResult;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportDataStream;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.server.core.model.widgets.ui.reporting.source.TileTemplateReportSource;
import cc.blynk.server.core.model.widgets.ui.reporting.type.DailyReport;
import cc.blynk.server.core.model.widgets.ui.reporting.type.DayOfMonth;
import cc.blynk.server.core.model.widgets.ui.reporting.type.MonthlyReport;
import cc.blynk.server.core.model.widgets.ui.reporting.type.OneTimeReport;
import cc.blynk.server.core.model.widgets.ui.reporting.type.ReportDurationType;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.MobileAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.FileUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static cc.blynk.integration.TestUtil.illegalCommand;
import static cc.blynk.integration.TestUtil.illegalCommandBody;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.sleep;
import static cc.blynk.server.core.model.widgets.ui.reporting.ReportOutput.CSV_FILE_PER_DEVICE;
import static cc.blynk.server.core.model.widgets.ui.reporting.ReportOutput.CSV_FILE_PER_DEVICE_PER_PIN;
import static cc.blynk.server.core.model.widgets.ui.reporting.ReportOutput.MERGED_CSV;
import static cc.blynk.server.core.model.widgets.ui.reporting.ReportResult.EXPIRED;
import static cc.blynk.server.core.model.widgets.ui.reporting.ReportResult.OK;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.enums.Response.QUOTA_LIMIT;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static java.nio.charset.StandardCharsets.UTF_16;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportingTest extends BaseTest {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        this.appServer = new MobileAndHttpsServer(holder).start();

        this.clientPair = initAppAndHardPair();
        reset(holder.mailWrapper);
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testDeleteAllDeviceData() throws Exception {
        Device device1 = new Device(2, "My Device2", BoardType.ESP8266);
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");

        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.DIGITAL, (short) 8, GraphGranularityType.MINUTE));
        Path pinReportingDataPath11 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.DIGITAL, (short) 8, GraphGranularityType.HOURLY));
        Path pinReportingDataPath12 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.DIGITAL, (short) 8, GraphGranularityType.DAILY));


        Path pinReportingDataPath20 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.DIGITAL, (short) 8, GraphGranularityType.MINUTE));

        FileUtils.write(pinReportingDataPath10, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath11, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath12, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath20, 1.22D, 2222222);

        clientPair.appClient.send("deleteDeviceData 1-*");
        clientPair.appClient.verifyResult(ok(2));

        assertTrue(Files.notExists(pinReportingDataPath10));
        assertTrue(Files.notExists(pinReportingDataPath11));
        assertTrue(Files.notExists(pinReportingDataPath12));
        assertTrue(Files.notExists(pinReportingDataPath20));
    }

    @Test
    public void testDeleteDeviceDataFor1Device() throws Exception {
        Device device1 = new Device(2, "My Device2", BoardType.ESP8266);
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");

        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.DIGITAL, (short) 8, GraphGranularityType.MINUTE));
        Path pinReportingDataPath11 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.DIGITAL, (short) 8, GraphGranularityType.HOURLY));
        Path pinReportingDataPath12 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.DIGITAL, (short) 8, GraphGranularityType.DAILY));


        Path pinReportingDataPath20 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.DIGITAL, (short) 8, GraphGranularityType.MINUTE));

        FileUtils.write(pinReportingDataPath10, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath11, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath12, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath20, 1.22D, 2222222);

        clientPair.appClient.deleteDeviceData(1, 2);
        clientPair.appClient.verifyResult(ok(2));

        assertTrue(Files.exists(pinReportingDataPath10));
        assertTrue(Files.exists(pinReportingDataPath11));
        assertTrue(Files.exists(pinReportingDataPath12));
        assertTrue(Files.notExists(pinReportingDataPath20));
    }

    @Test
    public void testDeleteDeviceDataForSpecificPin() throws Exception {
        Device device1 = new Device(2, "My Device2", BoardType.ESP8266);
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");

        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.DIGITAL, (short) 8, GraphGranularityType.MINUTE));
        Path pinReportingDataPath11 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.DIGITAL, (short) 8, GraphGranularityType.HOURLY));
        Path pinReportingDataPath12 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.DIGITAL, (short) 8, GraphGranularityType.DAILY));
        Path pinReportingDataPath13 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 9, GraphGranularityType.DAILY));


        Path pinReportingDataPath20 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.DIGITAL, (short) 8, GraphGranularityType.MINUTE));

        FileUtils.write(pinReportingDataPath10, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath11, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath12, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath13, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath20, 1.22D, 2222222);

        clientPair.appClient.deleteDeviceData(1, 2, "d8");
        clientPair.appClient.verifyResult(ok(2));

        assertTrue(Files.exists(pinReportingDataPath10));
        assertTrue(Files.exists(pinReportingDataPath11));
        assertTrue(Files.exists(pinReportingDataPath12));
        assertTrue(Files.exists(pinReportingDataPath13));
        assertTrue(Files.notExists(pinReportingDataPath20));

        clientPair.appClient.deleteDeviceData(1, 0, "d8", "v9");
        clientPair.appClient.verifyResult(ok(3));

        assertTrue(Files.notExists(pinReportingDataPath10));
        assertTrue(Files.notExists(pinReportingDataPath11));
        assertTrue(Files.notExists(pinReportingDataPath12));
        assertTrue(Files.notExists(pinReportingDataPath13));
        assertTrue(Files.notExists(pinReportingDataPath20));
    }

    @Test
    public void createReportCRUD() throws Exception {
        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(1, GET_ENERGY, "7500"));

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(3, GET_ENERGY, "7500"));

        Report report = new Report(1, "My One Time Report",
                new ReportSource[] {reportSource},
                new OneTimeReport(TimeUnit.DAYS.toMillis(1)), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, null, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(4);
        assertNotNull(report);

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(5, GET_ENERGY, "4600"));

        report = new Report(1, "Updated",
                new ReportSource[] {reportSource},
                new OneTimeReport(TimeUnit.DAYS.toMillis(1)), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, null, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.updateReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(6);
        assertNotNull(report);
        assertEquals("Updated", report.name);

        clientPair.appClient.deleteReport(1, report.id);
        clientPair.appClient.verifyResult(ok(7));

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(8, GET_ENERGY, "7500"));
    }

    @Test
    public void testDailyReportIsTriggered() throws Exception {
        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("addEnergy " + "100000" + "\0" + "1370-3990-1414-55681");
        clientPair.appClient.verifyResult(ok(2));

        //a bit upfront
        long now = System.currentTimeMillis() + 1500;

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, null, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(now, report.nextReportAt, 3000);

        Report report2 = new Report(2, "DailyReport2",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, now, now), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, null, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report2);

        report = clientPair.appClient.parseReportFromResponse(4);
        assertNotNull(report);
        assertEquals(now, report.nextReportAt, 3000);

        //expecting now is ignored as duration is INFINITE
        Report report3 = new Report(3, "DailyReport3",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, now + 86400_000, now + 86400_000), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, null, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report3);

        report = clientPair.appClient.parseReportFromResponse(5);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 3000);

        //now date is greater than end date, such reports are not accepted.
        Report report4 = new Report(4, "DailyReport4",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, now + 86400_000, now), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, null, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report4);

        clientPair.appClient.verifyResult(illegalCommand(6));

        //trigger date is tomorrow
        Report report5 = new Report(5, "DailyReport5",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.CUSTOM, now + 86400_000, now + 86400_000), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, null, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report5);

        report = clientPair.appClient.parseReportFromResponse(7);
        assertNotNull(report);
        assertEquals(now + 86400_000, report.nextReportAt, 3000);

        //report wit the same id is not allowed
        Report report6 = new Report(5, "DailyReport6",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.CUSTOM, now + 86400_000, now + 86400_000), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, null, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report6);

        clientPair.appClient.verifyResult(illegalCommand(8));

        int tries = 0;
        while (holder.reportScheduler.getCompletedTaskCount() < 3 && tries < 20) {
            sleep(100);
            tries++;
        }

        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"), eq("DailyReport"), any(), any());
        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"), eq("DailyReport2"), any(), any());
        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"), eq("DailyReport3"), any(), any());
        assertEquals(3, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(7, holder.reportScheduler.getTaskCount());
    }

    @Test
    public void testReportIdRemovedFromScheduler() throws Exception {
        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("addEnergy " + "100000" + "\0" + "1370-3990-1414-55681");
        clientPair.appClient.verifyResult(ok(2));

        //a bit upfront
        long now = System.currentTimeMillis() + 1000;

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, null, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 3000);

        Report report2 = new Report(2, "DailyReport2",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, now, now), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, null, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report2);

        report2 = clientPair.appClient.parseReportFromResponse(4);
        assertNotNull(report2);
        assertEquals(System.currentTimeMillis(), report2.nextReportAt, 3000);

        int tries = 0;
        while (holder.reportScheduler.getCompletedTaskCount() < 2 && tries < 20) {
            sleep(100);
            tries++;
        }

        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"), eq("DailyReport"), any(), any());
        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"), eq("DailyReport2"), any(), any());
        assertEquals(2, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(4, holder.reportScheduler.getTaskCount());

        clientPair.appClient.send("loadProfileGzipped 1");
        DashBoard dashBoard = clientPair.appClient.parseDash(5);
        assertNotNull(dashBoard);
        reportingWidget = dashBoard.getReportingWidget();
        assertNotNull(reportingWidget);

        assertEquals(ReportResult.NO_DATA, reportingWidget.reports[0].lastRunResult);
        assertEquals(ReportResult.NO_DATA, reportingWidget.reports[1].lastRunResult);

        clientPair.appClient.deleteReport(1, 1);
        clientPair.appClient.verifyResult(ok(6));

        assertEquals(3, holder.reportScheduler.getTaskCount());

        clientPair.appClient.deleteReport(1, 2);
        clientPair.appClient.verifyResult(ok(7));

        assertEquals(2, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());
        assertEquals(0, holder.reportScheduler.map.size());
    }

    @Test
    public void testReportIdRemovedFromSchedulerWhenDashIsRemoved() throws Exception {
        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("addEnergy " + "100000" + "\0" + "1370-3990-1414-55681");
        clientPair.appClient.verifyResult(ok(2));

        //a bit upfront
        long now = System.currentTimeMillis() + 1000;

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, null, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 3000);

        Report report2 = new Report(2, "DailyReport2",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, now, now), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, null, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report2);

        report2 = clientPair.appClient.parseReportFromResponse(4);
        assertNotNull(report2);
        assertEquals(System.currentTimeMillis(), report2.nextReportAt, 3000);

        int tries = 0;
        while (holder.reportScheduler.getCompletedTaskCount() < 2 && tries < 20) {
            sleep(100);
            tries++;
        }

        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"), eq("DailyReport"), any(), any());
        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"), eq("DailyReport2"), any(), any());
        assertEquals(2, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(4, holder.reportScheduler.getTaskCount());

        clientPair.appClient.deleteDash(1);
        clientPair.appClient.verifyResult(ok(5));

        assertEquals(2, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());
        assertEquals(0, holder.reportScheduler.map.size());
    }

    @Test
    public void testDailyReportWithSinglePointIsTriggeredAndNullName() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        long pointNow = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, pointNow);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        //a bit upfront
        long now = System.currentTimeMillis() + 1000;
        LocalTime localTime = LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of("UTC"));
        localTime = LocalTime.of(localTime.getHour(), localTime.getMinute());

        Report report = new Report(1, null,
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 5000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        String downloadUrl = "http://127.0.0.1:18080/" + filename;
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily Report is ready"),
                eq(downloadUrl),
                eq("Report name: Report<br>Period: Daily, at " + localTime));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        String resultCsvString = readStringFromFirstZipEntry(result);
        String[] split = resultCsvString.split("[,\n]");
        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(pointNow));
        assertEquals(nowFormatted, split[0]);
        assertEquals(1.11D, Double.parseDouble(split[1]), 0.0001);

        AsyncHttpClient httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent(null)
                        .setKeepAlive(true)
                        .build()
        );
        Future<Response> f = httpclient.prepareGet(downloadUrl).execute();
        Response response = f.get();
        assertEquals(200, response.getStatusCode());
        assertEquals("application/zip", response.getContentType());
        httpclient.close();
    }

    @Test
    public void testDailyReportWith24PointsCorrectlyFetched() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.HOURLY));
        long pointNow = System.currentTimeMillis();
        long pointNowTruncated = (pointNow / GraphGranularityType.HOURLY.period) * GraphGranularityType.HOURLY.period;
        pointNowTruncated -= TimeUnit.DAYS.toMillis(1);
        for (int i = 0; i < 24; i++) {
            FileUtils.write(pinReportingDataPath10, i, pointNowTruncated + TimeUnit.HOURS.toMillis(i));
        }

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        //a bit upfront
        long now = pointNow + 1000;
        LocalTime localTime = LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of("UTC"));
        localTime = LocalTime.of(localTime.getHour(), localTime.getMinute());

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.HOURLY, true, CSV_FILE_PER_DEVICE_PER_PIN,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 3500);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        String downloadUrl = "http://127.0.0.1:18080/" + filename;
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily DailyReport is ready"),
                eq(downloadUrl),
                eq("Report name: DailyReport<br>Period: Daily, at " + localTime));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        String resultCsvString = readStringFromFirstZipEntry(result);
        String[] split = resultCsvString.split("\n");
        assertEquals(24, split.length);
    }

    @Test
    public void testFinalFileNameCSVPerDevicePerPin() throws Exception {
        Device device1 = new Device(2, "My Device2 with big name", BoardType.ESP8266);
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        long pointNow = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, pointNow);

        Path pinReportingDataPath20 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        FileUtils.write(pinReportingDataPath20, 1.11D, pointNow);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature,yes", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0, 2}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(2));

        //a bit upfront
        long now = System.currentTimeMillis() + 1000;
        LocalTime localTime = LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of("UTC"));
        localTime = LocalTime.of(localTime.getHour(), localTime.getMinute());

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 3000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        String downloadUrl = "http://127.0.0.1:18080/" + filename;
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily DailyReport is ready"),
                eq(downloadUrl),
                eq("Report name: DailyReport<br>Period: Daily, at " + localTime));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        ZipFile zipFile = new ZipFile(result.toString());

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        assertTrue(entries.hasMoreElements());

        ZipEntry entry = entries.nextElement();
        assertNotNull(entry);
        assertEquals("MyDevice_0_Temperatureyes.csv", entry.getName());

        ZipEntry entry2 = entries.nextElement();
        assertNotNull(entry2);
        assertEquals("MyDevice2withbig_2_Temperatureyes.csv", entry2.getName());

    }

    @Test
    public void testFinalFileNameCSVPerDevice() throws Exception {
        Device device1 = new Device(2, "My Device2 with big name", BoardType.ESP8266);
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        long pointNow = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, pointNow);

        Path pinReportingDataPath20 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        FileUtils.write(pinReportingDataPath20, 1.11D, pointNow);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, null, true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0, 2}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(2));

        //a bit upfront
        long now = System.currentTimeMillis() + 1000;
        LocalTime localTime = LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of("UTC"));
        localTime = LocalTime.of(localTime.getHour(), localTime.getMinute());

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 3000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        String downloadUrl = "http://127.0.0.1:18080/" + filename;
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily DailyReport is ready"),
                eq(downloadUrl),
                eq("Report name: DailyReport<br>Period: Daily, at " + localTime));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        ZipFile zipFile = new ZipFile(result.toString());

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        assertTrue(entries.hasMoreElements());

        ZipEntry entry = entries.nextElement();
        assertNotNull(entry);
        assertEquals("MyDevice_0.csv", entry.getName());

        ZipEntry entry2 = entries.nextElement();
        assertNotNull(entry2);
        assertEquals("MyDevice2withbig_2.csv", entry2.getName());

        String resultCsvString = readStringFromFirstZipEntry(result);
        assertNotNull(resultCsvString);

        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(pointNow));
        assertEquals(resultCsvString, nowFormatted + ",v1,1.11\n");
    }

    @Test
    public void testFinalFileNameCSVPerDeviceUtf8() throws Exception {
        Device device1 = new Device(2, "My Device2 with big name", BoardType.ESP8266);
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        long pointNow = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, pointNow);

        Path pinReportingDataPath20 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        FileUtils.write(pinReportingDataPath20, 1.11D, pointNow);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Мій датастрім", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0, 2}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(2));

        //a bit upfront
        long now = System.currentTimeMillis() + 1000;
        LocalTime localTime = LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of("UTC"));
        localTime = LocalTime.of(localTime.getHour(), localTime.getMinute());

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 3000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        String downloadUrl = "http://127.0.0.1:18080/" + filename;
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily DailyReport is ready"),
                eq(downloadUrl),
                eq("Report name: DailyReport<br>Period: Daily, at " + localTime));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        ZipFile zipFile = new ZipFile(result.toString());

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        assertTrue(entries.hasMoreElements());

        ZipEntry entry = entries.nextElement();
        assertNotNull(entry);
        assertEquals("MyDevice_0.csv", entry.getName());

        ZipEntry entry2 = entries.nextElement();
        assertNotNull(entry2);
        assertEquals("MyDevice2withbig_2.csv", entry2.getName());

        String resultCsvString = readStringFromFirstZipEntry(result);
        assertNotNull(resultCsvString);

        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(pointNow));
        assertEquals(nowFormatted + ",Мій датастрім,1.11\n", resultCsvString);
    }

    @Test
    public void testFinalFileNameCSVPerDevice2() throws Exception {
        Device device1 = new Device(2, "My Device2 with big name", BoardType.ESP8266);
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        Path pinReportingDataPath20 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 2, GraphGranularityType.MINUTE));
        long pointNow = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, pointNow);
        FileUtils.write(pinReportingDataPath20, 1.12D, pointNow);

        Path pinReportingDataPath12 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        Path pinReportingDataPath22 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 2, GraphGranularityType.MINUTE));
        FileUtils.write(pinReportingDataPath12, 1.13D, pointNow);
        FileUtils.write(pinReportingDataPath22, 1.14D, pointNow);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, null, true);
        ReportDataStream reportDataStream2 = new ReportDataStream((short) 2, PinType.VIRTUAL, null, true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream, reportDataStream2},
                1,
                new int[] {0, 2}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(2));

        //a bit upfront
        long now = System.currentTimeMillis() + 1000;
        LocalTime localTime = LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of("UTC"));
        localTime = LocalTime.of(localTime.getHour(), localTime.getMinute());

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 3000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        String downloadUrl = "http://127.0.0.1:18080/" + filename;
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily DailyReport is ready"),
                eq(downloadUrl),
                eq("Report name: DailyReport<br>Period: Daily, at " + localTime));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        ZipFile zipFile = new ZipFile(result.toString());

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        assertTrue(entries.hasMoreElements());

        ZipEntry entry = entries.nextElement();
        assertNotNull(entry);
        assertEquals("MyDevice_0.csv", entry.getName());

        ZipEntry entry2 = entries.nextElement();
        assertNotNull(entry2);
        assertEquals("MyDevice2withbig_2.csv", entry2.getName());

        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(pointNow));

        String resultCsvString = readStringFromZipEntry(zipFile, entry);
        assertNotNull(resultCsvString);
        assertEquals(resultCsvString, nowFormatted + ",v1,1.11\n" + nowFormatted + ",v2,1.12\n");

        String resultCsvString2 = readStringFromZipEntry(zipFile, entry2);
        assertNotNull(resultCsvString2);
        assertEquals(resultCsvString2, nowFormatted + ",v1,1.13\n" + nowFormatted + ",v2,1.14\n");
    }

    @Test
    public void testFinalFileNameCSVPerDeviceUnicode() throws Exception {
        Device device1 = new Device(2, "Мій девайс", BoardType.ESP8266);
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        long pointNow = System.currentTimeMillis();

        Path pinReportingDataPath12 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        Path pinReportingDataPath22 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 2, GraphGranularityType.MINUTE));
        FileUtils.write(pinReportingDataPath12, 1.13D, pointNow);
        FileUtils.write(pinReportingDataPath22, 1.14D, pointNow);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, null, true);
        ReportDataStream reportDataStream2 = new ReportDataStream((short) 2, PinType.VIRTUAL, null, true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream, reportDataStream2},
                1,
                new int[] {2}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(2));

        //a bit upfront
        long now = System.currentTimeMillis() + 1000;
        LocalTime localTime = LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of("UTC"));
        localTime = LocalTime.of(localTime.getHour(), localTime.getMinute());

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 3000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        String downloadUrl = "http://127.0.0.1:18080/" + filename;
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily DailyReport is ready"),
                eq(downloadUrl),
                eq("Report name: DailyReport<br>Period: Daily, at " + localTime));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        ZipFile zipFile = new ZipFile(result.toString());

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        assertTrue(entries.hasMoreElements());

        ZipEntry entry = entries.nextElement();
        assertNotNull(entry);
        assertEquals("Мійдевайс_2.csv", entry.getName());

        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(pointNow));

        String resultCsvString = readStringFromZipEntry(zipFile, entry);
        assertNotNull(resultCsvString);
        assertEquals(nowFormatted + ",v1,1.13\n" + nowFormatted + ",v2,1.14\n", resultCsvString);
    }

    @Test
    public void testFinalFileNameCSVPerDevice2DataStreamWithName() throws Exception {
        Device device1 = new Device(2, "My Device2 with big name", BoardType.ESP8266);
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        Path pinReportingDataPath20 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 2, GraphGranularityType.MINUTE));
        long pointNow = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, pointNow);
        FileUtils.write(pinReportingDataPath20, 1.12D, pointNow);

        Path pinReportingDataPath12 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        Path pinReportingDataPath22 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 2, GraphGranularityType.MINUTE));
        FileUtils.write(pinReportingDataPath12, 1.13D, pointNow);
        FileUtils.write(pinReportingDataPath22, 1.14D, pointNow);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportDataStream reportDataStream2 = new ReportDataStream((short) 2, PinType.VIRTUAL, "Humidity", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream, reportDataStream2},
                1,
                new int[] {0, 2}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(2));

        //a bit upfront
        long now = System.currentTimeMillis() + 1000;
        LocalTime localTime = LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of("UTC"));
        localTime = LocalTime.of(localTime.getHour(), localTime.getMinute());

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 2000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        String downloadUrl = "http://127.0.0.1:18080/" + filename;
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily DailyReport is ready"),
                eq(downloadUrl),
                eq("Report name: DailyReport<br>Period: Daily, at " + localTime));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        ZipFile zipFile = new ZipFile(result.toString());

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        assertTrue(entries.hasMoreElements());

        ZipEntry entry = entries.nextElement();
        assertNotNull(entry);
        assertEquals("MyDevice_0.csv", entry.getName());

        ZipEntry entry2 = entries.nextElement();
        assertNotNull(entry2);
        assertEquals("MyDevice2withbig_2.csv", entry2.getName());

        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(pointNow));

        String resultCsvString = readStringFromZipEntry(zipFile, entry);
        assertNotNull(resultCsvString);
        assertEquals(resultCsvString, nowFormatted + ",Temperature,1.11\n" + nowFormatted + ",Humidity,1.12\n");

        String resultCsvString2 = readStringFromZipEntry(zipFile, entry2);
        assertNotNull(resultCsvString2);
        assertEquals(resultCsvString2, nowFormatted + ",Temperature,1.13\n" + nowFormatted + ",Humidity,1.14\n");
    }

    @Test
    public void testFinalFileNameCSVMerged2DataStreamWithName() throws Exception {
        Device device1 = new Device(2, "My Device2 with big name", BoardType.ESP8266);
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        Path pinReportingDataPath20 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 2, GraphGranularityType.MINUTE));
        long pointNow = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, pointNow);
        FileUtils.write(pinReportingDataPath20, 1.12D, pointNow);

        Path pinReportingDataPath12 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        Path pinReportingDataPath22 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 2, GraphGranularityType.MINUTE));
        FileUtils.write(pinReportingDataPath12, 1.13D, pointNow);
        FileUtils.write(pinReportingDataPath22, 1.14D, pointNow);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportDataStream reportDataStream2 = new ReportDataStream((short) 2, PinType.VIRTUAL, "Humidity", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream, reportDataStream2},
                1,
                new int[] {0, 2}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(2));

        //a bit upfront
        long now = System.currentTimeMillis() + 1000;
        LocalTime localTime = LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of("UTC"));
        localTime = LocalTime.of(localTime.getHour(), localTime.getMinute());

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, MERGED_CSV,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 2000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        String downloadUrl = "http://127.0.0.1:18080/" + filename;
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily DailyReport is ready"),
                eq(downloadUrl),
                eq("Report name: DailyReport<br>Period: Daily, at " + localTime));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        ZipFile zipFile = new ZipFile(result.toString());

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        assertTrue(entries.hasMoreElements());

        ZipEntry entry = entries.nextElement();
        assertNotNull(entry);
        assertEquals("DailyReport.csv", entry.getName());

        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(pointNow));

        String resultCsvString = readStringFromZipEntry(zipFile, entry);
        assertNotNull(resultCsvString);
        assertEquals(resultCsvString, nowFormatted + ",Temperature,My Device,1.11\n" + nowFormatted + ",Humidity,My Device,1.12\n"
                +                     nowFormatted + ",Temperature,My Device2 with big name,1.13\n" + nowFormatted + ",Humidity,My Device2 with big name,1.14\n");
    }

    @Test
    public void testFinalFileNameCSVMerged2DataStreamWithNameCorrectEscaping() throws Exception {
        Device device1 = new Device(2, "My Device2 with \"big\" name", BoardType.ESP8266);
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        Path pinReportingDataPath20 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 2, GraphGranularityType.MINUTE));
        long pointNow = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, pointNow);
        FileUtils.write(pinReportingDataPath20, 1.12D, pointNow);

        Path pinReportingDataPath12 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        Path pinReportingDataPath22 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 2, GraphGranularityType.MINUTE));
        FileUtils.write(pinReportingDataPath12, 1.13D, pointNow);
        FileUtils.write(pinReportingDataPath22, 1.14D, pointNow);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportDataStream reportDataStream2 = new ReportDataStream((short) 2, PinType.VIRTUAL, "Humidity", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream, reportDataStream2},
                1,
                new int[] {0, 2}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(2));

        //a bit upfront
        long now = System.currentTimeMillis() + 1000;
        LocalTime localTime = LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of("UTC"));
        localTime = LocalTime.of(localTime.getHour(), localTime.getMinute());

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, MERGED_CSV,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 2000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        String downloadUrl = "http://127.0.0.1:18080/" + filename;
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily DailyReport is ready"),
                eq(downloadUrl),
                eq("Report name: DailyReport<br>Period: Daily, at " + localTime));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        ZipFile zipFile = new ZipFile(result.toString());

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        assertTrue(entries.hasMoreElements());

        ZipEntry entry = entries.nextElement();
        assertNotNull(entry);
        assertEquals("DailyReport.csv", entry.getName());

        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(pointNow));

        String resultCsvString = readStringFromZipEntry(zipFile, entry);
        assertNotNull(resultCsvString);
        assertEquals(resultCsvString, nowFormatted + ",Temperature,My Device,1.11\n" + nowFormatted + ",Humidity,My Device,1.12\n"
                +                     nowFormatted + ",Temperature,\"My Device2 with \"\"big\"\" name\",1.13\n" + nowFormatted + ",Humidity,\"My Device2 with \"\"big\"\" name\",1.14\n");
    }

    @Test
    public void testFinalFileNameCSVMerged2DataStreamWithNameCorrectEscaping2() throws Exception {
        Device device1 = new Device(2, "My Device2 with, big name", BoardType.ESP8266);
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        Path pinReportingDataPath20 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 2, GraphGranularityType.MINUTE));
        long pointNow = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, pointNow);
        FileUtils.write(pinReportingDataPath20, 1.12D, pointNow);

        Path pinReportingDataPath12 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        Path pinReportingDataPath22 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 2, PinType.VIRTUAL, (short) 2, GraphGranularityType.MINUTE));
        FileUtils.write(pinReportingDataPath12, 1.13D, pointNow);
        FileUtils.write(pinReportingDataPath22, 1.14D, pointNow);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportDataStream reportDataStream2 = new ReportDataStream((short) 2, PinType.VIRTUAL, "Humidity", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream, reportDataStream2},
                1,
                new int[] {0, 2}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(2));

        //a bit upfront
        long now = System.currentTimeMillis() + 1000;
        LocalTime localTime = LocalTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of("UTC"));
        localTime = LocalTime.of(localTime.getHour(), localTime.getMinute());

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, MERGED_CSV,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 5000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        String downloadUrl = "http://127.0.0.1:18080/" + filename;
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily DailyReport is ready"),
                eq(downloadUrl),
                eq("Report name: DailyReport<br>Period: Daily, at " + localTime));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        ZipFile zipFile = new ZipFile(result.toString());

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        assertTrue(entries.hasMoreElements());

        ZipEntry entry = entries.nextElement();
        assertNotNull(entry);
        assertEquals("DailyReport.csv", entry.getName());

        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(pointNow));

        String resultCsvString = readStringFromZipEntry(zipFile, entry);
        assertNotNull(resultCsvString);
        assertEquals(resultCsvString, nowFormatted + ",Temperature,My Device,1.11\n" + nowFormatted + ",Humidity,My Device,1.12\n"
                +                     nowFormatted + ",Temperature,\"My Device2 with, big name\",1.13\n" + nowFormatted + ",Humidity,\"My Device2 with, big name\",1.14\n");
    }

    @Test
    public void testDailyReportWithSinglePointIsTriggeredAndExpired() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        long pointNow = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, pointNow);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, null, true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.id = 222222;
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        //a bit upfront
        long now = System.currentTimeMillis() + 1500;

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.CUSTOM, now, now), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 3000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily DailyReport is ready"),
                eq("http://127.0.0.1:18080/" + filename),
                any());
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(1, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        String resultCsvString = readStringFromFirstZipEntry(result);
        String[] split = resultCsvString.split("[,\n]");
        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(pointNow));
        assertEquals(nowFormatted, split[0]);
        assertEquals(1.11D, Double.parseDouble(split[1]), 0.0001);

        clientPair.appClient.getWidget(1, 222222);
        ReportingWidget reportingWidget2 = (ReportingWidget) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(reportingWidget2);
        assertEquals(EXPIRED, reportingWidget2.reports[0].lastRunResult);
    }

    @Test
    public void testOneTimeReportIsTriggered() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        long now = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, now);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        Report report = new Report(1, "OneTime Report",
                new ReportSource[] {reportSource},
                new OneTimeReport(TimeUnit.DAYS.toMillis(1)), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(0, report.lastReportAt);

        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"),
                any(),
                any(),
                any());

        clientPair.appClient.exportReport(1, 1);
        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(System.currentTimeMillis(), report.lastReportAt, 2000);
        assertEquals(OK, report.lastRunResult);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your one time OneTime Report is ready"),
                eq("http://127.0.0.1:18080/" + filename),
                eq("Report name: OneTime Report<br>Period: One time"));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(1, holder.reportScheduler.getTaskCount());
        assertEquals(0, holder.reportScheduler.map.size());
        assertEquals(0, holder.reportScheduler.getActiveCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        String resultCsvString = readStringFromFirstZipEntry(result);
        assertNotNull(resultCsvString);
        String[] split = resultCsvString.split("[,\n]");
        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(now));
        assertEquals(nowFormatted, split[0]);
        assertEquals(1.11D, Double.parseDouble(split[1]), 0.0001);
    }

    @Test
    public void testOneTimeReportWithWrongSources() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        long now = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, now);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );
        ReportSource reportSource2 = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        Report report = new Report(1, "OneTime Report",
                new ReportSource[] {reportSource, reportSource2},
                new OneTimeReport(TimeUnit.DAYS.toMillis(1)), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(0, report.lastReportAt);

        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"),
                any(),
                any(),
                any());

        clientPair.appClient.exportReport(1, 1);
        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(System.currentTimeMillis(), report.lastReportAt, 2000);
        assertEquals(OK, report.lastRunResult);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your one time OneTime Report is ready"),
                eq("http://127.0.0.1:18080/" + filename),
                eq("Report name: OneTime Report<br>Period: One time"));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(1, holder.reportScheduler.getTaskCount());
        assertEquals(0, holder.reportScheduler.map.size());
        assertEquals(0, holder.reportScheduler.getActiveCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        String resultCsvString = readStringFromFirstZipEntry(result);
        assertNotNull(resultCsvString);
        String[] split = resultCsvString.split("[,\n]");
        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(now));
        assertEquals(nowFormatted, split[0]);
        assertEquals(1.11D, Double.parseDouble(split[1]), 0.0001);
    }

    @Test
    public void testMultipleReceiversFroOneTimeReport() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        long now = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, now);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        Report report = new Report(1, "OneTime Report",
                new ReportSource[] {reportSource},
                new OneTimeReport(TimeUnit.DAYS.toMillis(1)), "test@gmail.com,test2@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(0, report.lastReportAt);

        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com,test2@gmail.com"),
                any(),
                any(),
                any());

        clientPair.appClient.exportReport(1, 1);
        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(System.currentTimeMillis(), report.lastReportAt, 2000);
        assertEquals(OK, report.lastRunResult);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com,test2@gmail.com"),
                eq("Your one time OneTime Report is ready"),
                eq("http://127.0.0.1:18080/" + filename),
                eq("Report name: OneTime Report<br>Period: One time"));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(1, holder.reportScheduler.getTaskCount());
        assertEquals(0, holder.reportScheduler.map.size());
        assertEquals(0, holder.reportScheduler.getActiveCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        String resultCsvString = readStringFromFirstZipEntry(result);
        assertNotNull(resultCsvString);
        String[] split = resultCsvString.split("[,\n]");
        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(now));
        assertEquals(nowFormatted, split[0]);
        assertEquals(1.11D, Double.parseDouble(split[1]), 0.0001);
    }

    @Test
    public void testStreamsAreCorrectlyFiltered() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath11 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        Path pinReportingDataPath12 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 2, GraphGranularityType.MINUTE));
        long now = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath11, 1.11D, now);
        FileUtils.write(pinReportingDataPath12, 1.12D, now);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportDataStream reportDataStream2 = new ReportDataStream((short) 2, PinType.VIRTUAL, "Temperature2", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream, reportDataStream2},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", false);
        Report report = new Report(1, "OneTime Report",
                new ReportSource[] {new TileTemplateReportSource(
                        new ReportDataStream[] {reportDataStream, reportDataStream2},
                        1,
                        new int[] {0}
                )},
                new OneTimeReport(TimeUnit.DAYS.toMillis(1)), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(0, report.lastReportAt);

        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"),
                any(),
                any(),
                any());

        clientPair.appClient.exportReport(1, 1);
        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(System.currentTimeMillis(), report.lastReportAt, 2000);
        assertEquals(OK, report.lastRunResult);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your one time OneTime Report is ready"),
                eq("http://127.0.0.1:18080/" + filename),
                eq("Report name: OneTime Report<br>Period: One time"));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(1, holder.reportScheduler.getTaskCount());
        assertEquals(0, holder.reportScheduler.map.size());
        assertEquals(0, holder.reportScheduler.getActiveCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        String resultCsvString = readStringFromFirstZipEntry(result);
        assertNotNull(resultCsvString);
        String[] split = resultCsvString.split("[,\n]");
        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(now));
        assertEquals(2, split.length);
        assertEquals(nowFormatted, split[0]);
        assertEquals(1.12D, Double.parseDouble(split[1]), 0.0001);
    }

    @Test
    public void testExportIsLimited() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        long now = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, now);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        Report report = new Report(1, "OneTime Report",
                new ReportSource[] {reportSource},
                new OneTimeReport(TimeUnit.DAYS.toMillis(1)), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(0, report.lastReportAt);

        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"),
                any(),
                any(),
                any());

        clientPair.appClient.exportReport(1, 1);
        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(System.currentTimeMillis(), report.lastReportAt, 2000);
        assertEquals(OK, report.lastRunResult);

        clientPair.appClient.exportReport(1, 1);
        clientPair.appClient.verifyResult(new ResponseMessage(4, QUOTA_LIMIT));
    }

    @Test
    public void testOneTimeReportIsTriggeredWithAnotherFormat() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        long now = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.11D, now);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        Report report = new Report(1, "OneTime Report",
                new ReportSource[] {reportSource},
                new OneTimeReport(TimeUnit.DAYS.toMillis(1)), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN,
                Format.TS, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(0, report.lastReportAt);

        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"),
                any(),
                any(),
                any());

        clientPair.appClient.exportReport(1, 1);
        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(System.currentTimeMillis(), report.lastReportAt, 2000);
        assertEquals(OK, report.lastRunResult);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your one time OneTime Report is ready"),
                eq("http://127.0.0.1:18080/" + filename),
                eq("Report name: OneTime Report<br>Period: One time"));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(1, holder.reportScheduler.getTaskCount());
        assertEquals(0, holder.reportScheduler.map.size());
        assertEquals(0, holder.reportScheduler.getActiveCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        String resultCsvString = readStringFromFirstZipEntry(result);
        assertNotNull(resultCsvString);
        String[] split = resultCsvString.split("[,\n]");
        assertEquals(now, Long.parseLong(split[0]), 2000);
        assertEquals(1.11D, Double.parseDouble(split[1]), 0.0001);
    }

    @Test
    public void testOneTimeReportIsTriggeredWithCustomJson() throws Exception {
        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.createReport(1, "{\"id\":12838,\"name\":\"Report\",\"reportSources\":[{\"type\":\"TILE_TEMPLATE\",\"templateId\":11844,\"deviceIds\":[0],\"reportDataStreams\":[{\"pin\":1,\"pinType\":\"VIRTUAL\",\"label\":\"Temperature\",\"isSelected\":true},{\"pin\":0,\"pinType\":\"VIRTUAL\",\"label\":\"Humidity\",\"isSelected\":true},{\"pin\":2,\"pinType\":\"VIRTUAL\",\"label\":\"Heat\",\"isSelected\":true}]}],\"reportType\":{\"type\":\"ONE_TIME\",\"rangeMillis\":86400000},\"recipients\":\"alexkipar@gmail.com\",\"granularityType\":\"HOURLY\",\"isActive\":true,\"reportOutput\":\"CSV_FILE_PER_DEVICE_PER_PIN\",\"tzName\":\"Europe/Kiev\",\"nextReportAt\":0,\"lastReportAt\":1528309698795,\"lastRunResult\":\"ERROR\"}");
        Report report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);

        clientPair.appClient.exportReport(1, 12838);
        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(System.currentTimeMillis(), report.lastReportAt, 2000);
        assertEquals(ReportResult.NO_DATA, report.lastRunResult);
    }

    @Test
    public void testOneTimeReportIsTriggeredAndNoData() throws Exception {
        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        Report report = new Report(1, "OneTime Report",
                new ReportSource[] {reportSource},
                new OneTimeReport(TimeUnit.DAYS.toMillis(1)), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN, null, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(0, report.lastReportAt);

        clientPair.appClient.exportReport(1, 1);
        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(System.currentTimeMillis(), report.lastReportAt, 2000);
        assertEquals(ReportResult.NO_DATA, report.lastRunResult);

        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"),
                any(),
                any(),
                any());
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(1, holder.reportScheduler.getTaskCount());
        assertEquals(0, holder.reportScheduler.map.size());
        assertEquals(0, holder.reportScheduler.getActiveCount());
    }

    @Test
    public void testOneTimeReportIsTriggeredAndNoData2() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        FileUtils.write(pinReportingDataPath10, 1.11D, 111111);


        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        Report report = new Report(1, "OneTime Report",
                new ReportSource[] {reportSource},
                new OneTimeReport(TimeUnit.DAYS.toMillis(1)), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN, null, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(0, report.lastReportAt);

        clientPair.appClient.exportReport(1, 1);
        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(System.currentTimeMillis(), report.lastReportAt, 2000);
        assertEquals(ReportResult.NO_DATA, report.lastRunResult);

        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"),
                any(),
                any(),
                any());
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(1, holder.reportScheduler.getTaskCount());
        assertEquals(0, holder.reportScheduler.map.size());
        assertEquals(0, holder.reportScheduler.getActiveCount());
    }

    @Test
    public void testExpiredReportIsNotAddedToTheProject() throws Exception {
        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.id = 222222;
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        long now = System.currentTimeMillis() + 1500;

        Report report = new Report(1, "MonthlyReport",
                new ReportSource[] {reportSource},
                new MonthlyReport(now, ReportDurationType.CUSTOM, now, now, DayOfMonth.FIRST), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN, null, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        clientPair.appClient.verifyResult(illegalCommandBody(2));

        clientPair.appClient.getWidget(1, 222222);
        ReportingWidget reportingWidget2 = (ReportingWidget) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(reportingWidget2);
        assertEquals(0, reportingWidget2.reports.length);


        verify(holder.mailWrapper, never()).sendReportEmail(eq("test@gmail.com"),
                any(),
                any(),
                any());
        sleep(200);
        assertEquals(0, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(0, holder.reportScheduler.getTaskCount());
        assertEquals(0, holder.reportScheduler.map.size());
        assertEquals(0, holder.reportScheduler.getActiveCount());
    }

    @Test
    public void testExpiredReportIsNotAddedToTheProject2() throws Exception {
        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, "Temperature", true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.id = 222222;
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        long now = System.currentTimeMillis() + 1500;

        Report report = new Report(1, "MonthlyReport",
                new ReportSource[] {reportSource},
                new MonthlyReport(now, ReportDurationType.CUSTOM, now, now + 30L * 86400_000, DayOfMonth.FIRST), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN, null, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);

        report = new Report(1, "MonthlyReport2",
                new ReportSource[] {reportSource},
                new MonthlyReport(now, ReportDurationType.CUSTOM, now, now, DayOfMonth.FIRST), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN, null, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.updateReport(1, report);
        clientPair.appClient.verifyResult(illegalCommandBody(3));

        clientPair.appClient.getWidget(1, 222222);
        ReportingWidget reportingWidget2 = (ReportingWidget) JsonParser.parseWidget(clientPair.appClient.getBody(4), 0);
        assertNotNull(reportingWidget2);
        assertEquals(1, reportingWidget2.reports.length);
        assertEquals("MonthlyReport", reportingWidget2.reports[0].name);
    }

    @Test
    public void testDailyReportWithSinglePointIsTriggeredAndOneRecordIsFiltered() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", getUserName());
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", getUserName(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 1, GraphGranularityType.MINUTE));
        long pointNow = System.currentTimeMillis();
        FileUtils.write(pinReportingDataPath10, 1.12D, pointNow - TimeUnit.HOURS.toMillis(25));
        FileUtils.write(pinReportingDataPath10, 1.11D, pointNow);

        ReportDataStream reportDataStream = new ReportDataStream((short) 1, PinType.VIRTUAL, null, true);
        ReportSource reportSource = new TileTemplateReportSource(
                new ReportDataStream[] {reportDataStream},
                1,
                new int[] {0}
        );

        ReportingWidget reportingWidget = new ReportingWidget();
        reportingWidget.id = 222222;
        reportingWidget.height = 1;
        reportingWidget.width = 1;
        reportingWidget.reportSources = new ReportSource[] {
                reportSource
        };

        clientPair.appClient.createWidget(1, reportingWidget);
        clientPair.appClient.verifyResult(ok(1));

        //a bit upfront
        long now = System.currentTimeMillis() + 1500;

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN,
                Format.ISO_SIMPLE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 3000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = getUserName() + "_Blynk_" + report.id + "_" + date + ".zip";
        verify(holder.mailWrapper, timeout(3000)).sendReportEmail(eq("test@gmail.com"),
                eq("Your daily DailyReport is ready"),
                eq("http://127.0.0.1:18080/" + filename),
                any());
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                getUserName() + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".zip");
        assertTrue(Files.exists(result));
        String resultCsvString = readStringFromFirstZipEntry(result);
        String[] split = resultCsvString.split("[,\n]");
        String nowFormatted = DateTimeFormatter
                .ofPattern(Format.ISO_SIMPLE.pattern)
                .withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(pointNow));
        assertEquals(nowFormatted, split[0]);
        assertEquals(1.11D, Double.parseDouble(split[1]), 0.0001);

        clientPair.appClient.getWidget(1, 222222);
        ReportingWidget reportingWidget2 = (ReportingWidget) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(reportingWidget2);
        assertEquals(OK, reportingWidget2.reports[0].lastRunResult);
    }

    private String readStringFromFirstZipEntry(Path path) throws Exception {
        ZipFile zipFile = new ZipFile(path.toString());

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        if (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            return readStringFromZipEntry(zipFile, entry);
        }
        throw new RuntimeException("Error reading result gzip file " + path.toString());
    }

    private String readStringFromZipEntry(ZipFile zipFile, ZipEntry entry) throws Exception {
        try (BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry))) {
            return new String(bis.readAllBytes(), UTF_16);
        }
    }
}




