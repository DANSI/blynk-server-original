package cc.blynk.integration;

import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.SlackWrapper;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.sms.SMSWrapper;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import cc.blynk.utils.properties.ServerProperties;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mock;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.InflaterInputStream;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.01.16.
 */
public abstract class BaseTest {

    protected static final String blynkTempDir;
    protected static final String DEFAULT_TEST_USER = "dima@mail.ua";

    static {
        Security.addProvider(new BouncyCastleProvider());
        blynkTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "blynk").toString();
    }

    public static ServerProperties properties;

    //tcp app/hardware ports
    public static int tcpHardPort;

    public Holder holder;
    @Mock
    public BlockingIOProcessor blockingIOProcessor;
    @Mock
    public TwitterWrapper twitterWrapper;
    @Mock
    public MailWrapper mailWrapper;
    @Mock
    public GCMWrapper gcmWrapper;
    @Mock
    public SMSWrapper smsWrapper;
    @Mock
    public SlackWrapper slackWrapper;

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            //we can ignore it
        }
    }

    public class MyHostVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }


    @BeforeClass
    public static void initProps() {
        properties = new ServerProperties(Collections.emptyMap());
        tcpHardPort = properties.getHttpPort();
    }

    @SuppressWarnings("unchecked")
    protected static List<String> consumeJsonPinValues(String response) {
        return JsonParser.readAny(response, List.class);
    }

    @SuppressWarnings("unchecked")
    protected static List<String> consumeJsonPinValues(CloseableHttpResponse response) throws IOException {
        return JsonParser.readAny(consumeText(response), List.class);
    }

    @After
    public void closeTransport() {
        holder.close();
    }

    @SuppressWarnings("unchecked")
    protected static String consumeText(CloseableHttpResponse response) throws IOException {
        return EntityUtils.toString(response.getEntity());
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

    protected static String getFileNameByMask(String whereToFind, String pattern) {
        File dir = new File(whereToFind);
        File[] files = dir.listFiles((dir1, name) -> name.startsWith(pattern));
        return latest(files).getName();
    }

    protected SSLContext initUnsecuredSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) {

            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) {

            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{ tm }, null);

        return context;
    }

    public static ClientPair initAppAndHardPair() throws Exception {
        return TestUtil.initAppAndHardPair("localhost", properties.getHttpsPort(), tcpHardPort, getUserName(), "1", null, properties, 10000);
    }

    public String getDataFolder() {
        return TestUtil.getDataFolder();
    }

    private static File latest(File[] files) {
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        return choice;
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

    public static ClientPair initAppAndHardPair(int energy) throws Exception {
        return TestUtil.initAppAndHardPair("localhost", properties.getHttpsPort(), tcpHardPort, getUserName(), "1", null, properties, energy);
    }

    public static ClientPair initAppAndHardPair(String jsonProfile) throws Exception {
        return TestUtil.initAppAndHardPair("localhost", properties.getHttpsPort(), tcpHardPort, getUserName(), "1", jsonProfile, properties, 10000);
    }

    public static ClientPair initAppAndHardPair(int tcpAppPort, int tcpHartPort, ServerProperties properties) throws Exception {
        return TestUtil.initAppAndHardPair("localhost", tcpAppPort, tcpHartPort, getUserName(), "1", null, properties, 10000);
    }

    public static ClientPair initAppAndHardPair(ServerProperties properties) throws Exception {
        return TestUtil.initAppAndHardPair("localhost", properties.getHttpsPort(), properties.getHttpPort(), getUserName(), "1", null, properties, 10000);
    }

    //generates unique name of a user, so every test is independent from others
    //name is unique only within the test
    public static String getUserName() {
        return userCounter.get() + DEFAULT_TEST_USER;
    }

    @Before
    public void initHolderAndDataFolder() {
        properties.setProperty("data.folder", getDataFolder());

        this.holder = new Holder(properties,
                twitterWrapper, mailWrapper,
                gcmWrapper, smsWrapper, slackWrapper, "no-db.properties");

        userCounter.incrementAndGet();
    }

    private static final AtomicLong userCounter = new AtomicLong();
}

