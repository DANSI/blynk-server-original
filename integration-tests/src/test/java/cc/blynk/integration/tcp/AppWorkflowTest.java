package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.enums.ProvisionType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AppWorkflowTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppServer(holder).start();

        this.clientPair = initAppAndHardPair();
        Files.deleteIfExists(Paths.get(getDataFolder(), "blynk", "userProfiles"));
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testPrintApp() throws Exception {
        App app = new App("1", Theme.Blynk, ProvisionType.STATIC, 0, false, "My App", "myIcon", new int[] {1});
        System.out.println(JsonParser.mapper.writeValueAsString(app));
    }

    @Test
    public void testAppCreated() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"isMultiFace\":true,\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody());
        assertNotNull(app);
        assertNotNull(app.id);
        assertEquals(13, app.id.length());
        assertEquals(Theme.Blynk, app.theme);
        assertEquals(ProvisionType.STATIC, app.provisionType);
        assertEquals(0, app.color);
        assertEquals("My App", app.name);
        assertEquals("myIcon", app.icon);
        assertTrue(app.isMultiFace);
        assertArrayEquals(new int[]{1}, app.projectIds);
    }

    @Test
    public void testAppCreated2() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody());
        assertNotNull(app);
        assertNotNull(app.id);

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[2]}");
        app = JsonParser.parseApp(clientPair.appClient.getBody(2));
        assertNotNull(app);
        assertNotNull(app.id);
    }

    @Test
    public void testUnicodeName() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"Моя апка\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody());
        assertNotNull(app);
        assertNotNull(app.id);
        assertEquals("Моя апка", app.name);
    }

    @Test
    public void testCantCreateWithSameId() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody());
        assertNotNull(app);
        assertNotNull(app.id);

        clientPair.appClient.send("createApp {\"id\":\"" + app.id + "\",\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[2]}");
        app = JsonParser.parseApp(clientPair.appClient.getBody(2));
        assertNotNull(app);
        assertNotNull(app.id);
    }

    @Test
    public void testAppUpdated() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody());
        assertNotNull(app);
        assertNotNull(app.id);

        clientPair.appClient.send("updateApp {\"id\":\"" + app.id  + "\",\"theme\":\"BlynkLight\",\"provisionType\":\"DYNAMIC\",\"color\":1,\"name\":\"My App 2\",\"icon\":\"myIcon2\",\"projectIds\":[1,2]}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("loadProfileGzipped");
        String s = clientPair.appClient.getBody(3);

        Profile profile = parseProfile(s);
        assertNotNull(profile);

        assertNotNull(profile.apps);
        assertEquals(1, profile.apps.length);
        App app2 = profile.apps[0];
        assertEquals(app.id, app2.id);
        assertEquals(Theme.BlynkLight, app2.theme);
        assertEquals(ProvisionType.DYNAMIC, app2.provisionType);
        assertEquals(1, app2.color);
        assertEquals("My App 2", app2.name);
        assertEquals("myIcon2", app2.icon);
        assertArrayEquals(new int[]{1, 2}, app2.projectIds);
    }

    @Test
    public void testAppDelete() throws Exception {
        clientPair.appClient.send("createApp {\"id\":1,\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody());
        assertNotNull(app);
        assertNotNull(app.id);

        clientPair.appClient.send("deleteApp " + app.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("loadProfileGzipped");
        String s = clientPair.appClient.getBody(3);

        Profile profile = parseProfile(s);
        assertNotNull(profile);

        assertNotNull(profile.apps);
        assertEquals(0, profile.apps.length);
    }
}
