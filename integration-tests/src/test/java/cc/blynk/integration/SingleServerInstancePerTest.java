package cc.blynk.integration;

import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.Holder;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.MobileAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.properties.ServerProperties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.Collections;

import static cc.blynk.integration.TestUtil.createDefaultHolder;
import static org.mockito.Mockito.reset;

/**
 * use when you need only 1 server instance per test class and not per test method
 */
public abstract class SingleServerInstancePerTest extends CounterBase {

    protected static ServerProperties properties;
    protected static BaseServer appServer;
    protected static BaseServer hardwareServer;
    protected static Holder holder;
    protected ClientPair clientPair;

    @BeforeClass
    public static void init() throws Exception {
        properties = new ServerProperties(Collections.emptyMap());
        properties.setProperty("data.folder", TestUtil.getDataFolder());
        holder = createDefaultHolder(properties, "no-db.properties");
        hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        appServer = new MobileAndHttpsServer(holder).start();
    }

    @AfterClass
    public static void shutdown() {
        appServer.close();
        hardwareServer.close();
        holder.close();
    }

    @After
    public void closeClients() {
        this.clientPair.stop();
    }

    @Before
    public void resetBeforeTest() throws Exception {
        this.clientPair = initClientPair();
        reset(holder.mailWrapper);
        reset(holder.twitterWrapper);
        reset(holder.gcmWrapper);
        reset(holder.smsWrapper);
    }

    public ClientPair initAppAndHardPair() throws Exception {
        return TestUtil.initAppAndHardPair("localhost",
                properties.getHttpsPort(), properties.getHttpPort(),
                getUserName(), "1", changeProfileTo(), properties, 10000);
    }

    protected String changeProfileTo() {
        return "user_profile_json.txt";
    }

    protected ClientPair initClientPair() throws Exception {
        return initAppAndHardPair();
    }

}
