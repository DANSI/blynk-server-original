package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.enums.ProvisionType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.serialization.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Paths;

import static cc.blynk.integration.TestUtil.ok;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AppWorkflowTest extends SingleServerInstancePerTest {

    @Before
    public void deleteFolder() throws Exception {
        Files.deleteIfExists(Paths.get(getDataFolder(), "blynk", "userProfiles"));
    }

    @Test
    public void testPrintApp() throws Exception {
        App app = new App("1", Theme.Blynk, ProvisionType.STATIC, 0, false, "My App", "myIcon", new int[] {1});
        System.out.println(JsonParser.MAPPER.writeValueAsString(app));
    }

    @Test
    public void testAppCreated() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"isMultiFace\":true,\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = clientPair.appClient.parseApp(1);
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
        App app = clientPair.appClient.parseApp(1);
        assertNotNull(app);
        assertNotNull(app.id);

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[2]}");
        app = clientPair.appClient.parseApp(2);
        assertNotNull(app);
        assertNotNull(app.id);
    }

    @Test
    public void testUnicodeName() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"Моя апка\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = clientPair.appClient.parseApp(1);
        assertNotNull(app);
        assertNotNull(app.id);
        assertEquals("Моя апка", app.name);
    }

    @Test
    public void testCantCreateWithSameId() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = clientPair.appClient.parseApp(1);
        assertNotNull(app);
        assertNotNull(app.id);

        clientPair.appClient.send("createApp {\"id\":\"" + app.id + "\",\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[2]}");
        app = clientPair.appClient.parseApp(2);
        assertNotNull(app);
        assertNotNull(app.id);
    }

    @Test
    public void testAppUpdated() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = clientPair.appClient.parseApp(1);
        assertNotNull(app);
        assertNotNull(app.id);

        clientPair.appClient.send("updateApp {\"id\":\"" + app.id  + "\",\"theme\":\"BlynkLight\",\"provisionType\":\"DYNAMIC\",\"color\":1,\"name\":\"My App 2\",\"icon\":\"myIcon2\",\"projectIds\":[1,2]}");
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(3);
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
        App app = clientPair.appClient.parseApp(1);
        assertNotNull(app);
        assertNotNull(app.id);

        clientPair.appClient.send("deleteApp " + app.id);
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.reset();

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);
        assertNotNull(profile);

        assertNotNull(profile.apps);
        assertEquals(0, profile.apps.length);
        assertEquals(0, profile.dashBoards.length);
    }
}
