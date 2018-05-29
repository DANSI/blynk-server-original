package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportDataStream;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.server.core.model.widgets.ui.reporting.source.TileTemplateReportSource;
import cc.blynk.server.core.model.widgets.ui.reporting.type.OneTimeReportType;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.AppAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;

import static cc.blynk.server.core.model.widgets.ui.reporting.ReportOutput.CSV_FILE_PER_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertTrue;

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
                ReportingDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.MINUTE));
        Path pinReportingDataPath11 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.HOURLY));
        Path pinReportingDataPath12 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.DAILY));


        Path pinReportingDataPath20 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingDao.generateFilename(1, 2, PinType.DIGITAL, (byte) 8, GraphGranularityType.MINUTE));

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
                ReportingDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.MINUTE));
        Path pinReportingDataPath11 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.HOURLY));
        Path pinReportingDataPath12 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.DAILY));


        Path pinReportingDataPath20 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingDao.generateFilename(1, 2, PinType.DIGITAL, (byte) 8, GraphGranularityType.MINUTE));

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
                ReportingDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.MINUTE));
        Path pinReportingDataPath11 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.HOURLY));
        Path pinReportingDataPath12 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingDao.generateFilename(1, 0, PinType.DIGITAL, (byte) 8, GraphGranularityType.DAILY));
        Path pinReportingDataPath13 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingDao.generateFilename(1, 0, PinType.VIRTUAL, (byte) 9, GraphGranularityType.DAILY));


        Path pinReportingDataPath20 = Paths.get(tempDir, "data", DEFAULT_TEST_USER,
                ReportingDao.generateFilename(1, 2, PinType.DIGITAL, (byte) 8, GraphGranularityType.MINUTE));

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
                new OneTimeReportType(86400), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, ZoneId.of("UTC"));

        clientPair.appClient.createReport(1, report);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(5, GET_ENERGY, "2600"));

        report = new Report(1, "Updated",
                new ReportSource[] {reportSource},
                new OneTimeReportType(86400), "test@gmail.com",
                GraphGranularityType.MINUTE, true, CSV_FILE_PER_DEVICE, ZoneId.of("UTC"));

        clientPair.appClient.updateReport(1, report);
        clientPair.appClient.verifyResult(ok(6));

        clientPair.appClient.deleteReport(1, report.id);
        clientPair.appClient.verifyResult(ok(7));

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(8, GET_ENERGY, "7500"));
    }
}




