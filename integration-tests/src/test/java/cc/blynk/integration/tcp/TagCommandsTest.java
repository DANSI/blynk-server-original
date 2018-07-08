package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.appSync;
import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.createTag;
import static cc.blynk.integration.TestUtil.illegalCommand;
import static cc.blynk.integration.TestUtil.ok;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TagCommandsTest extends SingleServerInstancePerTest {

    @Test
    public void testAddNewTag() throws Exception {
        Tag tag0 = new Tag(100_000, "Tag1");

        clientPair.appClient.createTag(1, tag0);
        String createdTag = clientPair.appClient.getBody();
        Tag tag = JsonParser.parseTag(createdTag, 0);
        assertNotNull(tag);
        assertEquals(100_000, tag.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createTag(1, tag)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getTags 1");
        String response = clientPair.appClient.getBody();

        Tag[] tags = JsonParser.MAPPER.readValue(response, Tag[].class);
        assertNotNull(tags);
        assertEquals(1, tags.length);

        assertEqualTag(tag0, tags[0]);
    }

    @Test
    public void testUpdateExistingDevice() throws Exception {
        Tag tag0 = new Tag(100_000, "Tag1");

        clientPair.appClient.createTag(1, tag0);
        String createdTag = clientPair.appClient.getBody();
        Tag tag = JsonParser.parseTag(createdTag, 0);
        assertNotNull(tag);
        assertEquals(100_000, tag.id);
        assertEquals("Tag1", tag.name);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createTag(1, tag)));

        clientPair.appClient.reset();

        tag0 = new Tag(100_000, "TagUPDATED");

        clientPair.appClient.updateTag(1, tag0);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.reset();

        clientPair.appClient.send("getTags 1");
        String response = clientPair.appClient.getBody();

        Tag[] tags = JsonParser.MAPPER.readValue(response, Tag[].class);
        assertNotNull(tags);
        assertEquals(1, tags.length);

        assertEqualTag(tag0, tags[0]);
    }

    @Test
    public void testUpdateNonExistingTag() throws Exception {
        Tag tag0 = new Tag(100_000, "Tag1");

        clientPair.appClient.updateTag(1, tag0);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(illegalCommand(1)));
    }

    @Test
    public void testUpdateTagWithSameName() throws Exception {
        Tag tag0 = new Tag(100_000, "Tag1");

        clientPair.appClient.createTag(1, tag0);
        String createdTag = clientPair.appClient.getBody();
        Tag tag = JsonParser.parseTag(createdTag, 0);
        assertNotNull(tag);
        assertEquals(100_000, tag.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createTag(1, tag)));

        clientPair.appClient.reset();

        Tag tag1 = new Tag(100_001, "Tag1");

        clientPair.appClient.createTag(1, tag1);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(illegalCommand(1)));
    }


    @Test
    public void testDeletedNewlyAddedTag() throws Exception {
        Tag tag0 = new Tag(100_000, "My Dashboard");

        clientPair.appClient.createTag(1, tag0);
        String createdTag = clientPair.appClient.getBody();
        Tag tag = JsonParser.parseTag(createdTag, 0);
        assertNotNull(tag);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createTag(1, tag0.toString())));

        clientPair.appClient.reset();

        clientPair.appClient.send("getTags 1");
        String response = clientPair.appClient.getBody();

        Tag[] tags = JsonParser.MAPPER.readValue(response, Tag[].class);
        assertNotNull(tags);
        assertEquals(1, tags.length);

        assertEqualTag(tag0, tags[0]);

        clientPair.appClient.deleteTag(1, tag0.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));


        clientPair.appClient.reset();

        clientPair.appClient.send("getTags 1");
        response = clientPair.appClient.getBody();
        tags = JsonParser.MAPPER.readValue(response, Tag[].class);

        assertNotNull(tags);
        assertEquals(0, tags.length);
    }

    @Test
    public void testAddNewTagForMultipleDevicesAndAssignWidgetAndVerifySync() throws Exception {
        //creating new device
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        TestHardClient hardClient2 = new TestHardClient("localhost", properties.getHttpPort());
        hardClient2.start();

        hardClient2.login(device.token);
        hardClient2.verifyResult(ok(1));
        clientPair.appClient.reset();

        //creating new tag
        Tag tag0 = new Tag(100_000, "Tag1");
        //assigning 2 devices on 1 tag.
        tag0.deviceIds = new int[] {0, 1};

        clientPair.appClient.createTag(1, tag0);
        String createdTag = clientPair.appClient.getBody();
        Tag tag = JsonParser.parseTag(createdTag, 0);
        assertNotNull(tag);
        assertEquals(100_000, tag.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createTag(1, tag)));

        clientPair.appClient.createWidget(1, "{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":100000, \"x\":0, \"y\":0, \"label\":\"Button\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("hardware 1-100000 vw 88 100");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(3, b("vw 88 100"))));
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(3, b("vw 88 100"))));

        clientPair.hardwareClient.sync(PinType.VIRTUAL, 88);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(1, b("vw 88 100"))));

        hardClient2.sync(PinType.VIRTUAL, 88);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(2, b("vw 88 100"))));

        clientPair.appClient.reset();

        clientPair.appClient.sync(1);

        verify(clientPair.appClient.responseMock, timeout(1000).times(14)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync(b("1-0 dw 1 1")));
        clientPair.appClient.verifyResult(appSync(b("1-0 dw 2 1")));
        clientPair.appClient.verifyResult(appSync(b("1-0 aw 3 0")));
        clientPair.appClient.verifyResult(appSync(b("1-0 dw 5 1")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 4 244")));
        clientPair.appClient.verifyResult(appSync(b("1-0 aw 7 3")));
        clientPair.appClient.verifyResult(appSync(b("1-0 aw 30 3")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 0 89.888037459418")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 11 -58.74774244674501")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 13 60 143 158")));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vw 88 100"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vw 88 100"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-100000 vw 88 100"))));

    }

    private static void assertEqualTag(Tag expected, Tag real) {
        assertEquals(expected.id, real.id);
        assertEquals(expected.name, real.name);
        assertArrayEquals(expected.deviceIds, real.deviceIds);
    }

}
