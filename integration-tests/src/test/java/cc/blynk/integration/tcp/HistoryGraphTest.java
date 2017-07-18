package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.EnhancedHistoryGraph;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphType;
import cc.blynk.server.core.protocol.model.messages.BinaryMessage;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.CreateDevice;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.ByteUtils;
import cc.blynk.utils.FileUtils;
import cc.blynk.utils.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class HistoryGraphTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppServer(holder).start();

        this.clientPair = initAppAndHardPair();
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testGetGraphDataFor1Pin() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");

        final Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath = Paths.get(tempDir, "data", DEFAULT_TEST_USER, ReportingDao.generateFilename(1, 0, PinType.DIGITAL.pintTypeChar, (byte) 8, GraphGranularityType.HOURLY));

        FileUtils.write(pinReportingDataPath, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath, 1.22D, 2222222);

        clientPair.appClient.send("getgraphdata 1 d 8 24 h");

        ArgumentCaptor<BinaryMessage> objectArgumentCaptor = ArgumentCaptor.forClass(BinaryMessage.class);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        BinaryMessage graphDataResponse = objectArgumentCaptor.getValue();

        assertNotNull(graphDataResponse);
        byte[] decompressedGraphData = ByteUtils.decompress(graphDataResponse.getBytes());
        ByteBuffer bb = ByteBuffer.wrap(decompressedGraphData);

        assertEquals(1, bb.getInt());
        assertEquals(2, bb.getInt());
        assertEquals(1.11D, bb.getDouble(), 0.1);
        assertEquals(1111111, bb.getLong());
        assertEquals(1.22D, bb.getDouble(), 0.1);
        assertEquals(2222222, bb.getLong());
    }

    @Test
    public void testDeleteGraphCommandWorks() throws Exception {
        clientPair.appClient.send("getgraphdata 1 d 8 del");

        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));
    }

    @Test
    public void testGetGraphDataForEnhancedGraphWithEmptyDataStream() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");

        final Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath = Paths.get(tempDir, "data", DEFAULT_TEST_USER, ReportingDao.generateFilename(1, 0, PinType.DIGITAL.pintTypeChar, (byte) 8, GraphGranularityType.HOURLY));

        FileUtils.write(pinReportingDataPath, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath, 1.22D, 2222222);

        EnhancedHistoryGraph enhancedHistoryGraph = new EnhancedHistoryGraph();
        enhancedHistoryGraph.id = 432;
        enhancedHistoryGraph.width = 8;
        enhancedHistoryGraph.height = 4;
        GraphDataStream graphDataStream = new GraphDataStream(null, GraphType.LINE, 0, 0, null, null, 0, 0, null, false, false, false);
        enhancedHistoryGraph.dataStreams = new GraphDataStream[] {
                graphDataStream
        };

        clientPair.appClient.send("createWidget 1" + "\0" + JsonParser.toJson(enhancedHistoryGraph));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getenhanceddata 1" + b(" 432 DAY"));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, NO_DATA)));
    }

    @Test
    public void testGetGraphDataForEnhancedGraph() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");

        final Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath = Paths.get(tempDir, "data", DEFAULT_TEST_USER, ReportingDao.generateFilename(1, 0, PinType.DIGITAL.pintTypeChar, (byte) 8, GraphGranularityType.HOURLY));

        FileUtils.write(pinReportingDataPath, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath, 1.22D, 2222222);

        EnhancedHistoryGraph enhancedHistoryGraph = new EnhancedHistoryGraph();
        enhancedHistoryGraph.id = 432;
        enhancedHistoryGraph.width = 8;
        enhancedHistoryGraph.height = 4;
        Pin pin = new Pin((byte) 8, PinType.DIGITAL);
        GraphDataStream graphDataStream = new GraphDataStream(null, GraphType.LINE, 0, 0, pin, null, 0, 0, null, false, false, false);
        enhancedHistoryGraph.dataStreams = new GraphDataStream[] {
                graphDataStream
        };

        clientPair.appClient.send("createWidget 1" + "\0" + JsonParser.toJson(enhancedHistoryGraph));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        clientPair.appClient.reset();

        clientPair.appClient.send("getenhanceddata 1" + b(" 432 DAY"));

        ArgumentCaptor<BinaryMessage> objectArgumentCaptor = ArgumentCaptor.forClass(BinaryMessage.class);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        BinaryMessage graphDataResponse = objectArgumentCaptor.getValue();

        assertNotNull(graphDataResponse);
        byte[] decompressedGraphData = ByteUtils.decompress(graphDataResponse.getBytes());
        ByteBuffer bb = ByteBuffer.wrap(decompressedGraphData);

        assertEquals(1, bb.getInt());
        assertEquals(2, bb.getInt());
        assertEquals(1.11D, bb.getDouble(), 0.1);
        assertEquals(1111111, bb.getLong());
        assertEquals(1.22D, bb.getDouble(), 0.1);
        assertEquals(2222222, bb.getLong());
    }

    @Test
    public void testPagingWorksForGetEnhancedHistoryDataPartialData() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");

        final Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath = Paths.get(tempDir, "data", DEFAULT_TEST_USER, ReportingDao.generateFilename(1, 0, PinType.DIGITAL.pintTypeChar, (byte) 8, GraphGranularityType.MINUTE));

        try (DataOutputStream dos = new DataOutputStream(
                    Files.newOutputStream(pinReportingDataPath, CREATE, APPEND))) {
            for (int i = 1; i <= 61; i++) {
                dos.writeDouble(i);
                dos.writeLong(i * 1000);
            }
            dos.flush();
        }

        EnhancedHistoryGraph enhancedHistoryGraph = new EnhancedHistoryGraph();
        enhancedHistoryGraph.id = 432;
        enhancedHistoryGraph.width = 8;
        enhancedHistoryGraph.height = 4;
        Pin pin = new Pin((byte) 8, PinType.DIGITAL);
        GraphDataStream graphDataStream = new GraphDataStream(null, GraphType.LINE, 0, 0, pin, null, 0, 0, null, false, false, false);
        enhancedHistoryGraph.dataStreams = new GraphDataStream[] {
                graphDataStream
        };

        clientPair.appClient.send("createWidget 1" + "\0" + JsonParser.toJson(enhancedHistoryGraph));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        clientPair.appClient.reset();

        clientPair.appClient.send("getenhanceddata 1" + b(" 432 ONE_HOUR 1"));

        ArgumentCaptor<BinaryMessage> objectArgumentCaptor = ArgumentCaptor.forClass(BinaryMessage.class);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        BinaryMessage graphDataResponse = objectArgumentCaptor.getValue();

        assertNotNull(graphDataResponse);
        byte[] decompressedGraphData = ByteUtils.decompress(graphDataResponse.getBytes());
        ByteBuffer bb = ByteBuffer.wrap(decompressedGraphData);


        assertEquals(1, bb.getInt());
        assertEquals(1, bb.getInt());
        assertEquals(1D, bb.getDouble(), 0.1);
        assertEquals(1000, bb.getLong());
    }

    @Test
    public void testPagingWorksForGetEnhancedHistoryDataFullData() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");

        final Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath = Paths.get(tempDir, "data", DEFAULT_TEST_USER, ReportingDao.generateFilename(1, 0, PinType.DIGITAL.pintTypeChar, (byte) 8, GraphGranularityType.MINUTE));

        try (DataOutputStream dos = new DataOutputStream(
                Files.newOutputStream(pinReportingDataPath, CREATE, APPEND))) {
            for (int i = 1; i <= 120; i++) {
                dos.writeDouble(i);
                dos.writeLong(i * 1000);
            }
            dos.flush();
        }

        EnhancedHistoryGraph enhancedHistoryGraph = new EnhancedHistoryGraph();
        enhancedHistoryGraph.id = 432;
        enhancedHistoryGraph.width = 8;
        enhancedHistoryGraph.height = 4;
        Pin pin = new Pin((byte) 8, PinType.DIGITAL);
        GraphDataStream graphDataStream = new GraphDataStream(null, GraphType.LINE, 0, 0, pin, null, 0, 0, null, false, false, false);
        enhancedHistoryGraph.dataStreams = new GraphDataStream[] {
                graphDataStream
        };

        clientPair.appClient.send("createWidget 1" + "\0" + JsonParser.toJson(enhancedHistoryGraph));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        clientPair.appClient.reset();

        clientPair.appClient.send("getenhanceddata 1" + b(" 432 ONE_HOUR 1"));

        ArgumentCaptor<BinaryMessage> objectArgumentCaptor = ArgumentCaptor.forClass(BinaryMessage.class);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        BinaryMessage graphDataResponse = objectArgumentCaptor.getValue();

        assertNotNull(graphDataResponse);
        byte[] decompressedGraphData = ByteUtils.decompress(graphDataResponse.getBytes());
        ByteBuffer bb = ByteBuffer.wrap(decompressedGraphData);


        assertEquals(1, bb.getInt());
        assertEquals(60, bb.getInt());
        for (int i = 1; i <= 60; i++) {
            assertEquals(i, bb.getDouble(), 0.1);
            assertEquals(i * 1000, bb.getLong());
        }
    }

    @Test
    public void testPagingWorksForGetEnhancedHistoryDataWhenNoData() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");

        final Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath = Paths.get(tempDir, "data", DEFAULT_TEST_USER, ReportingDao.generateFilename(1, 0, PinType.DIGITAL.pintTypeChar, (byte) 8, GraphGranularityType.MINUTE));

        try (DataOutputStream dos = new DataOutputStream(
                Files.newOutputStream(pinReportingDataPath, CREATE, APPEND))) {
            for (int i = 1; i <= 60; i++) {
                dos.writeDouble(i);
                dos.writeLong(i * 1000);
            }
            dos.flush();
        }

        EnhancedHistoryGraph enhancedHistoryGraph = new EnhancedHistoryGraph();
        enhancedHistoryGraph.id = 432;
        enhancedHistoryGraph.width = 8;
        enhancedHistoryGraph.height = 4;
        Pin pin = new Pin((byte) 8, PinType.DIGITAL);
        GraphDataStream graphDataStream = new GraphDataStream(null, GraphType.LINE, 0, 0, pin, null, 0, 0, null, false, false, false);
        enhancedHistoryGraph.dataStreams = new GraphDataStream[] {
                graphDataStream
        };

        clientPair.appClient.send("createWidget 1" + "\0" + JsonParser.toJson(enhancedHistoryGraph));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        clientPair.appClient.reset();

        clientPair.appClient.send("getenhanceddata 1" + b(" 432 ONE_HOUR 1"));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NO_DATA)));
    }

    @Test
    public void testDeleteWorksForEnhancedGraph() throws Exception {
        EnhancedHistoryGraph enhancedHistoryGraph = new EnhancedHistoryGraph();
        enhancedHistoryGraph.id = 432;
        enhancedHistoryGraph.width = 8;
        enhancedHistoryGraph.height = 4;
        Pin pin = new Pin((byte) 8, PinType.DIGITAL);
        GraphDataStream graphDataStream = new GraphDataStream(null, GraphType.LINE, 0, 0, pin, null, 0, 0, null, false, false, false);
        enhancedHistoryGraph.dataStreams = new GraphDataStream[] {
                graphDataStream
        };

        clientPair.appClient.send("createWidget 1" + "\0" + JsonParser.toJson(enhancedHistoryGraph));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("deleteEnhancedData 1\0" + "432");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));
        clientPair.appClient.reset();

        clientPair.appClient.send("getenhanceddata 1" + b(" 432 DAY"));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NO_DATA)));
    }

    @Test
    public void testExportDataFromHistoryGraph() throws Exception {
        clientPair.appClient.send("export 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));

        clientPair.appClient.send("export 1 666");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, ILLEGAL_COMMAND)));

        clientPair.appClient.send("export 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, ILLEGAL_COMMAND)));

        clientPair.appClient.send("export 1 14");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, NO_DATA)));

        //generate fake reporting data
        Path userReportDirectory = Paths.get(holder.props.getProperty("data.folder"), "data", DEFAULT_TEST_USER);
        Files.createDirectories(userReportDirectory);
        Path userReportFile = Paths.get(userReportDirectory.toString(), ReportingDao.generateFilename(1, 0, PinType.ANALOG.pintTypeChar, (byte) 7, GraphGranularityType.MINUTE));
        FileUtils.write(userReportFile, 1.1, 1L);
        FileUtils.write(userReportFile, 2.2, 2L);

        clientPair.appClient.send("export 1 14");
        verify(mailWrapper, timeout(1000)).sendHtml(eq(DEFAULT_TEST_USER), eq("History graph data for project My Dashboard"), contains("/dima@mail.ua_1_0_a7_"));
    }

    @Test
    public void testGeneratedCSVIsCorrect() throws Exception {
        //generate fake reporting data
        Path userReportDirectory = Paths.get(holder.props.getProperty("data.folder"), "data", DEFAULT_TEST_USER);
        Files.createDirectories(userReportDirectory);
        String filename = ReportingDao.generateFilename(1, 0, PinType.ANALOG.pintTypeChar, (byte) 7, GraphGranularityType.MINUTE);
        Path userReportFile = Paths.get(userReportDirectory.toString(), filename);
        FileUtils.write(userReportFile, 1.1, 1L);
        FileUtils.write(userReportFile, 2.2, 2L);

        clientPair.appClient.send("export 1 14");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        String csvFileName = getFileNameByMask("/tmp/blynk", "dima@mail.ua_1_0_a7_");
        verify(mailWrapper, timeout(1000)).sendHtml(eq(DEFAULT_TEST_USER), eq("History graph data for project My Dashboard"), contains(csvFileName));

        try (InputStream fileStream = new FileInputStream(Paths.get("/tmp/blynk", csvFileName).toString());
             InputStream gzipStream = new GZIPInputStream(fileStream);
             BufferedReader buffered = new BufferedReader(new InputStreamReader(gzipStream))) {

            String[] lineSplit = buffered.readLine().split(",");
            assertEquals(1.1D, Double.parseDouble(lineSplit[0]), 0.001D);
            assertEquals(1, Long.parseLong(lineSplit[1]));
            assertEquals(0, Long.parseLong(lineSplit[2]));

            lineSplit = buffered.readLine().split(",");
            assertEquals(2.2D, Double.parseDouble(lineSplit[0]), 0.001D);
            assertEquals(2, Long.parseLong(lineSplit[1]));
            assertEquals(0, Long.parseLong(lineSplit[2]));
        }
    }

    @Test
    public void testGeneratedCSVIsCorrectForMultiDevicesAndEnhancedGraph() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.send("createWidget 1\0{\"id\":200000, \"deviceIds\":[0,1], \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        EnhancedHistoryGraph enhancedHistoryGraph = new EnhancedHistoryGraph();
        enhancedHistoryGraph.id = 432;
        enhancedHistoryGraph.width = 8;
        enhancedHistoryGraph.height = 4;
        Pin pin = new Pin((byte) 8, PinType.DIGITAL);
        GraphDataStream graphDataStream = new GraphDataStream(null, GraphType.LINE, 0, 200_000, pin, null, 0, 0, null, false, false, false);
        enhancedHistoryGraph.dataStreams = new GraphDataStream[] {
                graphDataStream
        };

        clientPair.appClient.send("createWidget 1" + "\0" + JsonParser.toJson(enhancedHistoryGraph));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.appClient.reset();

        //generate fake reporting data
        Path userReportDirectory = Paths.get(holder.props.getProperty("data.folder"), "data", DEFAULT_TEST_USER);
        Files.createDirectories(userReportDirectory);

        String filename = ReportingDao.generateFilename(1, 0, PinType.DIGITAL.pintTypeChar, (byte) 8, GraphGranularityType.MINUTE);
        Path userReportFile = Paths.get(userReportDirectory.toString(), filename);
        FileUtils.write(userReportFile, 1.1, 1L);
        FileUtils.write(userReportFile, 2.2, 2L);

        filename = ReportingDao.generateFilename(1, 1, PinType.DIGITAL.pintTypeChar, (byte) 8, GraphGranularityType.MINUTE);
        userReportFile = Paths.get(userReportDirectory.toString(), filename);
        FileUtils.write(userReportFile, 11.1, 11L);
        FileUtils.write(userReportFile, 12.2, 12L);

        clientPair.appClient.send("export 1 432");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        String csvFileName = getFileNameByMask("/tmp/blynk", "dima@mail.ua_1_200000_d8_");
        verify(mailWrapper, timeout(1000)).sendHtml(eq(DEFAULT_TEST_USER), eq("History graph data for project My Dashboard"), contains(csvFileName));

        try (InputStream fileStream = new FileInputStream(Paths.get("/tmp/blynk", csvFileName).toString());
             InputStream gzipStream = new GZIPInputStream(fileStream);
             BufferedReader buffered = new BufferedReader(new InputStreamReader(gzipStream))) {

            //first device
            String[] lineSplit = buffered.readLine().split(",");
            assertEquals(1.1D, Double.parseDouble(lineSplit[0]), 0.001D);
            assertEquals(1, Long.parseLong(lineSplit[1]));
            assertEquals(0, Long.parseLong(lineSplit[2]));

            lineSplit = buffered.readLine().split(",");
            assertEquals(2.2D, Double.parseDouble(lineSplit[0]), 0.001D);
            assertEquals(2, Long.parseLong(lineSplit[1]));
            assertEquals(0, Long.parseLong(lineSplit[2]));

            //second device
            lineSplit = buffered.readLine().split(",");
            assertEquals(11.1D, Double.parseDouble(lineSplit[0]), 0.001D);
            assertEquals(11, Long.parseLong(lineSplit[1]));
            assertEquals(1, Long.parseLong(lineSplit[2]));

            lineSplit = buffered.readLine().split(",");
            assertEquals(12.2D, Double.parseDouble(lineSplit[0]), 0.001D);
            assertEquals(12, Long.parseLong(lineSplit[1]));
            assertEquals(1, Long.parseLong(lineSplit[2]));
        }
    }

    @Test
    public void testGeneratedCSVIsCorrectForMultiDevices() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.send("createWidget 1\0{\"id\":200000, \"deviceIds\":[0,1], \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        clientPair.appClient.send("updateWidget 1\0" + "{\n" +
                "                    \"type\":\"LOGGER\",\n" +
                "                    \"id\":14,\n" +
                "                    \"x\":0,\n" +
                "                    \"y\":6,\n" +
                "                    \"color\":0,\n" +
                "                    \"width\":8,\n" +
                "                    \"height\":3,\n" +
                "                    \"tabId\":0,\n" +
                "                    \"deviceId\":200000,\n" +
                "                    \"pins\":\n" +
                "                        [\n" +
                "                            {\"pinType\":\"ANALOG\", \"pin\":7,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":255},\n" +
                "                            {\"pin\":-1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0},\n" +
                "                            {\"pin\":-1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0},\n" +
                "                            {\"pin\":-1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0}\n" +
                "                        ],\n" +
                "                    \"period\":\"THREE_MONTHS\",\n" +
                "                    \"showLegends\":true\n" +
                "                }");

        clientPair.appClient.reset();

        //generate fake reporting data
        Path userReportDirectory = Paths.get(holder.props.getProperty("data.folder"), "data", DEFAULT_TEST_USER);
        Files.createDirectories(userReportDirectory);

        String filename = ReportingDao.generateFilename(1, 0, PinType.ANALOG.pintTypeChar, (byte) 7, GraphGranularityType.MINUTE);
        Path userReportFile = Paths.get(userReportDirectory.toString(), filename);
        FileUtils.write(userReportFile, 1.1, 1L);
        FileUtils.write(userReportFile, 2.2, 2L);

        filename = ReportingDao.generateFilename(1, 1, PinType.ANALOG.pintTypeChar, (byte) 7, GraphGranularityType.MINUTE);
        userReportFile = Paths.get(userReportDirectory.toString(), filename);
        FileUtils.write(userReportFile, 11.1, 11L);
        FileUtils.write(userReportFile, 12.2, 12L);

        clientPair.appClient.send("export 1 14");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        String csvFileName = getFileNameByMask("/tmp/blynk", "dima@mail.ua_1_200000_a7_");
        verify(mailWrapper, timeout(1000)).sendHtml(eq(DEFAULT_TEST_USER), eq("History graph data for project My Dashboard"), contains(csvFileName));

        try (InputStream fileStream = new FileInputStream(Paths.get("/tmp/blynk", csvFileName).toString());
             InputStream gzipStream = new GZIPInputStream(fileStream);
             BufferedReader buffered = new BufferedReader(new InputStreamReader(gzipStream))) {

            //first device
            String[] lineSplit = buffered.readLine().split(",");
            assertEquals(1.1D, Double.parseDouble(lineSplit[0]), 0.001D);
            assertEquals(1, Long.parseLong(lineSplit[1]));
            assertEquals(0, Long.parseLong(lineSplit[2]));

            lineSplit = buffered.readLine().split(",");
            assertEquals(2.2D, Double.parseDouble(lineSplit[0]), 0.001D);
            assertEquals(2, Long.parseLong(lineSplit[1]));
            assertEquals(0, Long.parseLong(lineSplit[2]));

            //second device
            lineSplit = buffered.readLine().split(",");
            assertEquals(11.1D, Double.parseDouble(lineSplit[0]), 0.001D);
            assertEquals(11, Long.parseLong(lineSplit[1]));
            assertEquals(1, Long.parseLong(lineSplit[2]));

            lineSplit = buffered.readLine().split(",");
            assertEquals(12.2D, Double.parseDouble(lineSplit[0]), 0.001D);
            assertEquals(12, Long.parseLong(lineSplit[1]));
            assertEquals(1, Long.parseLong(lineSplit[2]));
        }
    }

    @Test
    public void testGeneratedCSVIsCorrectForMultiDevicesNoData() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.send("createWidget 1\0{\"id\":200000, \"deviceIds\":[0,1], \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.reset();

        clientPair.appClient.send("export 1-200000 14");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NO_DATA)));
    }

}
