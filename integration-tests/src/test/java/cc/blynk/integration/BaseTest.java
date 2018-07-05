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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Collections;
import java.util.List;
import java.util.zip.InflaterInputStream;

import static cc.blynk.integration.TestUtil.DEFAULT_TEST_USER;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.01.16.
 */
public abstract class BaseTest {

    public static final String blynkTempDir;

    static {
        Security.addProvider(new BouncyCastleProvider());
        blynkTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "blynk").toString();
    }

    public static ServerProperties properties;

    //tcp app/hardware ports
    public static int tcpAppPort;
    public static int tcpHardPort;

    //http/s ports
    public static int httpPort;
    public static int httpsPort;

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


    public SSLContext initUnsecuredSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {

            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {

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

    @BeforeClass
    public static void initProps() {
        properties = new ServerProperties(Collections.emptyMap());

        httpPort = properties.getIntProperty("http.port");
        httpsPort = properties.getIntProperty("https.port");

        tcpAppPort = httpsPort;
        tcpHardPort = httpPort;
    }

    @Before
    public void initHolderAndDataFolder() throws Exception {
        if (getDataFolder() != null) {
            properties.setProperty("data.folder", getDataFolder());
        }

        this.holder = new Holder(properties,
                twitterWrapper, mailWrapper,
                gcmWrapper, smsWrapper, slackWrapper, "no-db.properties");
    }

    @After
    public void closeTransport() {
        holder.close();
    }

    public String getDataFolder() {
        try {
            return Files.createTempDirectory("blynk_test_", new FileAttribute[0]).toString();
        } catch (IOException e) {
            throw new RuntimeException("Unable create temp dir.", e);
        }
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

    @SuppressWarnings("unchecked")
    public static List<String> consumeJsonPinValues(String response) {
        return JsonParser.readAny(response, List.class);
    }

    @SuppressWarnings("unchecked")
    public static List<String> consumeJsonPinValues(CloseableHttpResponse response) throws IOException {
        return JsonParser.readAny(consumeText(response), List.class);
    }

    @SuppressWarnings("unchecked")
    public static String consumeText(CloseableHttpResponse response) throws IOException {
        return EntityUtils.toString(response.getEntity());
    }

    public static String getFileNameByMask(String whereToFind, String pattern) {
        File dir = new File(whereToFind);
        File[] files = dir.listFiles((dir1, name) -> name.startsWith(pattern));
        return latest(files).getName();
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

    public static ClientPair initAppAndHardPair() throws Exception {
        return TestUtil.initAppAndHardPair("localhost", tcpAppPort, tcpHardPort, DEFAULT_TEST_USER, "1", null, properties, 10000);
    }

    public static ClientPair initAppAndHardPair(int energy) throws Exception {
        return TestUtil.initAppAndHardPair("localhost", tcpAppPort, tcpHardPort, DEFAULT_TEST_USER, "1", null, properties, energy);
    }

    public static ClientPair initAppAndHardPair(String jsonProfile) throws Exception {
        return TestUtil.initAppAndHardPair("localhost", tcpAppPort, tcpHardPort, DEFAULT_TEST_USER, "1", jsonProfile, properties, 10000);
    }

    public static ClientPair initAppAndHardPair(int tcpAppPort, int tcpHartPort, ServerProperties properties) throws Exception {
        return TestUtil.initAppAndHardPair("localhost", tcpAppPort, tcpHartPort, DEFAULT_TEST_USER, "1", null, properties, 10000);
    }
}

