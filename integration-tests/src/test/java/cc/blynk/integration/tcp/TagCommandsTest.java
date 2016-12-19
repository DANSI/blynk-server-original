package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.CreateTag;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND;
import static cc.blynk.server.core.protocol.enums.Response.OK;
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
public class TagCommandsTest extends IntegrationBase {

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
    public void testAddNewTag() throws Exception {
        Tag tag0 = new Tag(100_000, "Tag1");

        clientPair.appClient.send("createTAg 1\0" + tag0.toString());
        String createdTag = clientPair.appClient.getBody();
        Tag tag = JsonParser.parseTag(createdTag);
        assertNotNull(tag);
        assertEquals(100_000, tag.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateTag(1, tag.toString())));

        clientPair.appClient.reset();

        clientPair.appClient.send("getTags 1");
        String response = clientPair.appClient.getBody();

        Tag[] tags = JsonParser.mapper.readValue(response, Tag[].class);
        assertNotNull(tags);
        assertEquals(1, tags.length);

        assertEqualTag(tag0, tags[0]);
    }

    @Test
    public void testUpdateExistingDevice() throws Exception {
        Tag tag0 = new Tag(100_000, "Tag1");

        clientPair.appClient.send("createTAg 1\0" + tag0.toString());
        String createdTag = clientPair.appClient.getBody();
        Tag tag = JsonParser.parseTag(createdTag);
        assertNotNull(tag);
        assertEquals(100_000, tag.id);
        assertEquals("Tag1", tag.name);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateTag(1, tag.toString())));

        clientPair.appClient.reset();

        tag0 = new Tag(100_000, "TagUPDATED");

        clientPair.appClient.send("updateTag 1\0" + tag0.toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getTags 1");
        String response = clientPair.appClient.getBody();

        Tag[] tags = JsonParser.mapper.readValue(response, Tag[].class);
        assertNotNull(tags);
        assertEquals(1, tags.length);

        assertEqualTag(tag0, tags[0]);
    }

    @Test
    public void testUpdateNonExistingTag() throws Exception {
        Tag tag0 = new Tag(100_000, "Tag1");

        clientPair.appClient.send("updateTag 1\0" + tag0.toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));
    }

    @Test
    public void testUpdateTagWithSameName() throws Exception {
        Tag tag0 = new Tag(100_000, "Tag1");

        clientPair.appClient.send("createTag 1\0" + tag0.toString());
        String createdTag = clientPair.appClient.getBody();
        Tag tag = JsonParser.parseTag(createdTag);
        assertNotNull(tag);
        assertEquals(100_000, tag.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateTag(1, tag.toString())));

        clientPair.appClient.reset();

        Tag tag1 = new Tag(100_001, "Tag1");

        clientPair.appClient.send("createTag 1\0" + tag1.toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));
    }


    @Test
    public void testDeletedNewlyAddedTag() throws Exception {
        Tag tag0 = new Tag(100_000, "My Dashboard");

        clientPair.appClient.send("createTag 1\0" + tag0.toString());
        String createdTag = clientPair.appClient.getBody();
        Tag tag = JsonParser.parseTag(createdTag);
        assertNotNull(tag);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateTag(1, tag0.toString())));

        clientPair.appClient.reset();

        clientPair.appClient.send("getTags 1");
        String response = clientPair.appClient.getBody();

        Tag[] tags = JsonParser.mapper.readValue(response, Tag[].class);
        assertNotNull(tags);
        assertEquals(1, tags.length);

        assertEqualTag(tag0, tags[0]);

        clientPair.appClient.send("deleteTag 1\0" + tag0.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));


        clientPair.appClient.reset();

        clientPair.appClient.send("getTags 1");
        response = clientPair.appClient.getBody();
        tags = JsonParser.mapper.readValue(response, Tag[].class);

        assertNotNull(tags);
        assertEquals(0, tags.length);
    }

    private static void assertEqualTag(Tag expected, Tag real) {
        assertEquals(expected.id, real.id);
        assertEquals(expected.name, real.name);
        assertArrayEquals(expected.deviceIds, real.deviceIds);
    }

}
