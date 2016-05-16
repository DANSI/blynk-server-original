package cc.blynk.integration;

import cc.blynk.server.Holder;
import cc.blynk.server.TransportTypeHolder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.sms.SMSWrapper;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.ServerProperties;
import org.apache.commons.lang.SystemUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.mockito.Mock;

import java.io.IOException;
import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.01.16.
 */
public abstract class BaseTest {

    public ServerProperties properties;

    public Holder holder;

    public TransportTypeHolder transportTypeHolder;

    //tcp app/hardware ports
    public int tcpAppPort;
    public int tcpHardPort;

    //http/s ports
    public int httpPort;
    public int httpsPort;

    //administration ports
    public int administrationPort;

    //web socket ports
    public int tcpWebSocketPort;
    public int sslWebSocketPort;

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

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            //we can ignore it
        }
    }

    @Before
    public void initBase() throws Exception {
        this.properties = new ServerProperties();

        //disable native linux epoll transport for non linux envs.
        if (!SystemUtils.IS_OS_LINUX) {
            System.out.println("WARNING : DISABLING NATIVE EPOLL TRANSPORT. SYSTEM : " + SystemUtils.OS_NAME);
            this.properties.put("enable.native.epoll.transport", false);
        }

        if (getDataFolder() != null) {
            this.properties.setProperty("data.folder", getDataFolder());
        }
        this.holder = new Holder(properties, twitterWrapper, mailWrapper, gcmWrapper, smsWrapper);
        this.transportTypeHolder = new TransportTypeHolder(this.properties);

        this.tcpAppPort = properties.getIntProperty("app.ssl.port");
        this.tcpHardPort = properties.getIntProperty("hardware.default.port");

        this.httpPort = properties.getIntProperty("http.port");
        this.httpsPort = properties.getIntProperty("https.port");

        this.administrationPort = properties.getIntProperty("administration.https.port");

        this.tcpWebSocketPort = properties.getIntProperty("tcp.websocket.port");
        this.sslWebSocketPort = properties.getIntProperty("ssl.websocket.port");
    }

    public String getDataFolder() {
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<String> consumeJsonPinValues(CloseableHttpResponse response) throws IOException {
        return JsonParser.readAny(consumeText(response), List.class);
    }

    @SuppressWarnings("unchecked")
    public String consumeText(CloseableHttpResponse response) throws IOException {
        return EntityUtils.toString(response.getEntity());
    }

}
