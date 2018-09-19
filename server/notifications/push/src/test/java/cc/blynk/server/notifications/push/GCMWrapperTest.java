package cc.blynk.server.notifications.push;

import cc.blynk.server.notifications.push.android.AndroidGCMMessage;
import cc.blynk.server.notifications.push.enums.Priority;
import cc.blynk.server.notifications.push.ios.IOSGCMMessage;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.properties.GCMProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.epoll.Epoll;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.06.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class GCMWrapperTest {

    private static final AsyncHttpClient client = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                .setUserAgent(null)
                .setKeepAlive(true)
                .setUseNativeTransport(Epoll.isAvailable())
            .build()
        );

    @AfterClass
    public static void closeHttpClient() throws Exception {
        client.close();
    }

    @Mock
    private GCMProperties props;

    @Test
    @Ignore
    public void testIOS() {
        GCMWrapper gcmWrapper = new GCMWrapper(props, client, AppNameUtil.BLYNK);
        gcmWrapper.send(new IOSGCMMessage("to", Priority.normal, "yo!!!", 1), null, null);
    }

    @Test
    @Ignore
    public void testAndroid() throws Exception {
        when(props.getProperty("gcm.api.key")).thenReturn("");
        when(props.getProperty("gcm.server")).thenReturn("");
        GCMWrapper gcmWrapper = new GCMWrapper(props, client, AppNameUtil.BLYNK);
        gcmWrapper.send(new AndroidGCMMessage("", Priority.normal, "yo!!!", 1), null, null);
        Thread.sleep(5000);
    }

    @Test
    public void testValidAndroidJson() throws JsonProcessingException {
        assertEquals("{\"to\":\"to\",\"priority\":\"normal\",\"data\":{\"message\":\"yo!!!\",\"dashId\":1}}", new AndroidGCMMessage("to", Priority.normal, "yo!!!", 1).toJson());
    }

    @Test
    public void testValidIOSJson() throws JsonProcessingException {
        IOSGCMMessage iosgcmMessage = new IOSGCMMessage("to", Priority.normal, "yo!!!", 1);
        iosgcmMessage.setTitle("Blynk Notification");
        assertEquals("{\"to\":\"to\",\"priority\":\"normal\",\"notification\":{\"body\":\"yo!!!\",\"dashId\":1,\"sound\":\"default\",\"title\":\"Blynk Notification\"}}", iosgcmMessage.toJson());
    }

}
