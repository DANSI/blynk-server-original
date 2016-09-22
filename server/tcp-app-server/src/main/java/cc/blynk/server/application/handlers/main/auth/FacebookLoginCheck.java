package cc.blynk.server.application.handlers.main.auth;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

/**
 * Simple class that verifies that facebook access token is correct and belongs to user that sent it.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.04.16.
 */
//todo not secure enough, I should check also app token belongs to
public class FacebookLoginCheck {

    private static final String URL = "https://graph.facebook.com/me?fields=email&access_token=TOKEN";

    private final DefaultAsyncHttpClient asyncHttpClient;
    private final ObjectReader facebookResponseReader = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readerFor(FacebookVerifyResponse.class);

    public FacebookLoginCheck(DefaultAsyncHttpClient asyncHttpClient) {
        this.asyncHttpClient = asyncHttpClient;
    }

    public void verify(String username, String token) throws Exception {
        asyncHttpClient.prepareGet(URL.replace("TOKEN", token))
                .execute(new AsyncCompletionHandler<Response>() {
                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        if (response.getStatusCode() == 200) {
                            FacebookVerifyResponse facebookVerifyResponse = facebookResponseReader.readValue(response.getResponseBody());
                            if (!username.equals(facebookVerifyResponse.email)) {
                                throw new IllegalArgumentException("Token is invalid. Facebook email " + facebookVerifyResponse.email + " != " + username);
                            }
                        } else {
                            throw new IllegalArgumentException(response.getResponseBody());
                        }
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        throw new IllegalArgumentException(t);
                    }
                });
    }

    private static class FacebookVerifyResponse {
        public String id;
        public String email;
    }

}
