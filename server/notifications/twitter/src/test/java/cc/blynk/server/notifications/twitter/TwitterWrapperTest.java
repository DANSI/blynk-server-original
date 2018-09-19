package cc.blynk.server.notifications.twitter;

import cc.blynk.utils.properties.TwitterProperties;
import io.netty.channel.epoll.Epoll;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.01.18.
 */
public class TwitterWrapperTest {

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

    @Test
    @Ignore("requires real credentials")
    public void testSend() throws Exception {
        TwitterProperties twitterProperties = new TwitterProperties(Collections.emptyMap());
        TwitterWrapper twitterWrapper = new TwitterWrapper(twitterProperties, client);

        String userToken = "";
        String userSecret = "";
        String message = "Hello!!!";

        twitterWrapper.send(userToken, userSecret, message,
                new AsyncCompletionHandler<>() {
                    @Override
                    public Response onCompleted(Response response) {
                        if (response.getStatusCode() != HttpResponseStatus.OK.code()) {
                            System.out.println("Error sending twit. Reason : " + response.getResponseBody());
                        }
                        assertEquals(200, response.getStatusCode());
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        t.printStackTrace();
                        System.out.println("Error sending twit.");
                    }
                });
        Thread.sleep(5000);
    }

}
