package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.core.dao.ReportingStorageDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportResult;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportDataStream;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.server.core.model.widgets.ui.reporting.source.TileTemplateReportSource;
import cc.blynk.server.core.model.widgets.ui.reporting.type.DailyReport;
import cc.blynk.server.core.model.widgets.ui.reporting.type.OneTimeReport;
import cc.blynk.server.core.model.widgets.ui.reporting.type.ReportDurationType;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.AppAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import static cc.blynk.server.core.model.widgets.ui.reporting.ReportOutput.CSV_FILE_PER_DEVICE;
import static cc.blynk.server.core.model.widgets.ui.reporting.ReportOutput.CSV_FILE_PER_DEVICE_PER_PIN;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
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
public class ReportingTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        this.appServer = new AppAndHttpsServer(holder).start();

        this.clientPair = initAppAndHardPair();
        reset(mailWrapper);
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testDeleteAllDeviceData() throws Exception {
        Device device1 = new Device(2, "My Device2", "ESP8266");
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");

        Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath10 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.MINUTE));
        Path pinReportingDataPath11 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.HOURLY));
        Path pinReportingDataPath12 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.DAILY));


        Path pinReportingDataPath20 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 2, PinType.DIGITAL, (byte) 8, GraphGranularityType.MINUTE));

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
        Device device1 = new Device(2, "My Device2", "ESP8266");
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");

        Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath10 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.MINUTE));
        Path pinReportingDataPath11 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.HOURLY));
        Path pinReportingDataPath12 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.DAILY));


        Path pinReportingDataPath20 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 2, PinType.DIGITAL, (byte) 8, GraphGranularityType.MINUTE));

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
        Device device1 = new Device(2, "My Device2", "ESP8266");
        clientPair.appClient.createDevice(1, device1);

        String tempDir = holder.props.getProperty("data.folder");

        Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath10 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.MINUTE));
        Path pinReportingDataPath11 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.HOURLY));
        Path pinReportingDataPath12 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.DAILY));
        Path pinReportingDataPath13 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 0, PinType.VIRTUAL, (byte) 9, GraphGranularityType.DAILY));


        Path pinReportingDataPath20 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 2, PinType.DIGITAL, (byte) 8, GraphGranularityType.MINUTE));

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
        ReportDataStream reportDataStream = new ReportDataStream((byte) 1, PinType.VIRTUAL, "Temperature", true);
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
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(4);
        assertNotNull(report);

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(5, GET_ENERGY, "2600"));

        report = new Report(1, "Updated",
                new ReportSource[] {reportSource},
                new OneTimeReport(TimeUnit.DAYS.toMillis(1)), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.updateReport(1, report);
        clientPair.appClient.verifyResult(ok(6));

        clientPair.appClient.deleteReport(1, report.id);
        clientPair.appClient.verifyResult(ok(7));

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(8, GET_ENERGY, "7500"));
    }

    @Test
    public void testDailyReportIsTriggered() throws Exception {
        ReportDataStream reportDataStream = new ReportDataStream((byte) 1, PinType.VIRTUAL, "Temperature", true);
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
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 2000);

        Report report2 = new Report(2, "DailyReport2",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, now, now), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report2);

        report = clientPair.appClient.parseReportFromResponse(4);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 2000);

        //expecting now is ignored as duration is INFINITE
        Report report3 = new Report(3, "DailyReport3",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, now + 86400_000, now + 86400_000), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report3);

        report = clientPair.appClient.parseReportFromResponse(5);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 2000);

        //now date is greater than end date, such reports are not accepted.
        Report report4 = new Report(4, "DailyReport4",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, now + 86400_000, now), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report4);

        clientPair.appClient.verifyResult(illegalCommand(6));

        //trigger date is tomorrow
        Report report5 = new Report(5, "DailyReport5",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.CUSTOM, now + 86400_000, now + 86400_000), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report5);

        report = clientPair.appClient.parseReportFromResponse(7);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis() + 86400_000, report.nextReportAt, 2000);

        //report wit the same id is not allowed
        Report report6 = new Report(5, "DailyReport6",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.CUSTOM, now + 86400_000, now + 86400_000), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report6);

        clientPair.appClient.verifyResult(illegalCommand(8));

        int tries = 0;
        while (holder.reportScheduler.getCompletedTaskCount() < 3 && tries < 20) {
            sleep(100);
            tries++;
        }

        verify(mailWrapper, never()).sendHtml(eq("test@gmail.com"), eq("DailyReport"), eq("Your report is ready."));
        verify(mailWrapper, never()).sendHtml(eq("test@gmail.com"), eq("DailyReport2"), eq("Your report is ready."));
        verify(mailWrapper, never()).sendHtml(eq("test@gmail.com"), eq("DailyReport3"), eq("Your report is ready."));
        assertEquals(3, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(7, holder.reportScheduler.getTaskCount());
    }

    @Test
    public void testReportIdRemovedFromScheduler() throws Exception {
        ReportDataStream reportDataStream = new ReportDataStream((byte) 1, PinType.VIRTUAL, "Temperature", true);
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
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 2000);

        Report report2 = new Report(2, "DailyReport2",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, now, now), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report2);

        report2 = clientPair.appClient.parseReportFromResponse(4);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 2000);

        int tries = 0;
        while (holder.reportScheduler.getCompletedTaskCount() < 2 && tries < 20) {
            sleep(100);
            tries++;
        }

        verify(mailWrapper, never()).sendHtml(eq("test@gmail.com"), eq("DailyReport"), any());
        verify(mailWrapper, never()).sendHtml(eq("test@gmail.com"), eq("DailyReport2"), any());
        assertEquals(2, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(4, holder.reportScheduler.getTaskCount());

        clientPair.appClient.send("loadProfileGzipped 1");
        DashBoard dashBoard = clientPair.appClient.getDash(5);
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
    public void testDailyReportWithSinglePointIsTriggered() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 0, PinType.VIRTUAL, (byte) 1, GraphGranularityType.MINUTE));
        FileUtils.write(pinReportingDataPath10, 1.11D, 1111111);

        ReportDataStream reportDataStream = new ReportDataStream((byte) 1, PinType.VIRTUAL, "Temperature", true);
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

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new DailyReport(now, ReportDurationType.INFINITE, 0, 0), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN, ZoneId.of("UTC"), 0, 0, null);
        clientPair.appClient.createReport(1, report);

        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(System.currentTimeMillis(), report.nextReportAt, 2000);

        String date = LocalDate.now(report.tzName).toString();
        String filename = DEFAULT_TEST_USER + "_Blynk_" + report.id + "_" + date + ".gz";
        verify(mailWrapper, timeout(3000)).sendHtml(eq("test@gmail.com"),
                eq("Your report " + report.name + " is ready!"),
                eq("<html><body><a href=\"http://127.0.0.1:18080/" + filename + "\">DailyReport</a><br></body></html>"));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(2, holder.reportScheduler.getTaskCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                DEFAULT_TEST_USER + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".gz");
        assertTrue(Files.exists(result));
        assertEquals(146, Files.size(result));
    }

    @Test
    public void testOneTimeReportIsTriggered() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }
        Path pinReportingDataPath10 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingStorageDao.generateFilename(1, 0, PinType.VIRTUAL, (byte) 1, GraphGranularityType.MINUTE));
        FileUtils.write(pinReportingDataPath10, 1.11D, 1111111);

        ReportDataStream reportDataStream = new ReportDataStream((byte) 1, PinType.VIRTUAL, "Temperature", true);
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

        Report report = new Report(1, "DailyReport",
                new ReportSource[] {reportSource},
                new OneTimeReport(TimeUnit.DAYS.toMillis(1)), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE_PER_PIN, ZoneId.of("UTC"), 0, 0, null);

        clientPair.appClient.createReport(1, report);
        report = clientPair.appClient.parseReportFromResponse(2);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(0, report.lastReportAt);

        verify(mailWrapper, never()).sendHtml(eq("test@gmail.com"),
                any(),
                any());

        clientPair.appClient.exportReport(1, 1);
        report = clientPair.appClient.parseReportFromResponse(3);
        assertNotNull(report);
        assertEquals(0, report.nextReportAt);
        assertEquals(System.currentTimeMillis(), report.lastReportAt, 2000);
        assertEquals(ReportResult.OK, report.lastRunResult);

        String date = LocalDate.now(report.tzName).toString();
        String filename = DEFAULT_TEST_USER + "_Blynk_" + report.id + "_" + date + ".gz";
        verify(mailWrapper, timeout(3000)).sendHtml(eq("test@gmail.com"),
                eq("Your report " + report.name + " is ready!"),
                eq("<html><body><a href=\"http://127.0.0.1:18080/" + filename + "\">DailyReport</a><br></body></html>"));
        sleep(200);
        assertEquals(1, holder.reportScheduler.getCompletedTaskCount());
        assertEquals(1, holder.reportScheduler.getTaskCount());
        assertEquals(0, holder.reportScheduler.map.size());
        assertEquals(0, holder.reportScheduler.getActiveCount());

        Path result = Paths.get(FileUtils.CSV_DIR,
                DEFAULT_TEST_USER + "_" + AppNameUtil.BLYNK + "_" + report.id + "_" + date + ".gz");
        assertTrue(Files.exists(result));
        assertEquals(146, Files.size(result));
    }
}




