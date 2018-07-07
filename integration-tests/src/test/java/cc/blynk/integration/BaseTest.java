package cc.blynk.integration;

import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.Holder;
import cc.blynk.utils.properties.ServerProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.zip.InflaterInputStream;

import static cc.blynk.integration.TestUtil.createDefaultHolder;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.01.16.
 */
public abstract class BaseTest extends CounterBase {

    public static ServerProperties properties;

    //tcp app/hardware ports
    public static int tcpHardPort;

    public Holder holder;

    @Before
    public void initHolderAndDataFolder() {
        properties.setProperty("data.folder", getDataFolder());
        this.holder = createDefaultHolder(properties, "no-db.properties");
    }

    @BeforeClass
    public static void initProps() {
        properties = new ServerProperties(Collections.emptyMap());
        tcpHardPort = properties.getHttpPort();
    }

    @After
    public void closeTransport() {
        holder.close();
    }

    public static String getRelativeDataFolder(String path) {
        URL resource = BaseTest.class.getResource(path);
        URI uri = null;
        try {
            uri = resource.toURI();
        } catch (Exception e) {
            //ignoring. that's fine.
        }
        String resourcesPath = Paths.get(uri).toAbsolutePath().toString();
        System.out.println("Resource path : " + resourcesPath);
        return resourcesPath;
    }

    public static ClientPair initAppAndHardPair() throws Exception {
        return TestUtil.initAppAndHardPair("localhost", properties.getHttpsPort(), tcpHardPort, getUserName(), "1", "user_profile_json.txt", properties, 10000);
    }

    //for tests only
    public static byte[] decompress(byte[] bytes) {
        try (InputStream in = new InflaterInputStream(new ByteArrayInputStream(bytes))) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static ClientPair initAppAndHardPair(String jsonProfile) throws Exception {
        return TestUtil.initAppAndHardPair("localhost", properties.getHttpsPort(), tcpHardPort, getUserName(), "1", jsonProfile, properties, 10000);
    }

    public static ClientPair initAppAndHardPair(int tcpAppPort, int tcpHartPort, ServerProperties properties) throws Exception {
        return TestUtil.initAppAndHardPair("localhost", tcpAppPort, tcpHartPort, getUserName(), "1", "user_profile_json.txt", properties, 10000);
    }

    public static ClientPair initAppAndHardPair(ServerProperties properties) throws Exception {
        return TestUtil.initAppAndHardPair("localhost", properties.getHttpsPort(), properties.getHttpPort(), getUserName(), "1", "user_profile_json.txt", properties, 10000);
    }

}

