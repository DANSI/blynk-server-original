package cc.blynk.server.notifications.mail.http;

import cc.blynk.server.notifications.mail.MailClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;

import java.util.Properties;
import java.util.concurrent.Future;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.09.16.
 */
public class HttpMailClient implements MailClient {

    private static final Logger log = LogManager.getLogger(HttpMailClient.class);

    private final AsyncHttpClient httpclient;
    private final String apiUrl;
    private final String apiKey;
    private final String from;
    private final ObjectWriter httpMailWriter;

    public HttpMailClient(Properties mailProperties, AsyncHttpClient asyncHttpClient) {
        this.httpclient = asyncHttpClient;
        this.apiUrl = mailProperties.getProperty("mail.host");
        this.apiKey = mailProperties.getProperty("mail.api.key");
        this.from = mailProperties.getProperty("mail.from");
        this.httpMailWriter = new ObjectMapper().writerFor(MailBody.class);
        log.info("Initializing Http mail transport. Username : {}. Provider url : {}", from, apiUrl);
    }

    @Override
    public void sendText(String to, String subj, String body) throws Exception {
        send(new MailBody(from, subj, body, to, false));
    }

    @Override
    public void sendHtml(String to, String subj, String body) throws Exception {
        send(new MailBody(from, subj, body, to, true));
    }

    private void send(MailBody mailBody) throws Exception {
        Future<Response> f = httpclient.preparePost(apiUrl)
                .setHeader("Authorization", apiKey)
                .setHeader("Content-Type", "application/json")
                .setBody(httpMailWriter.writeValueAsString(mailBody))
                .execute();
        Response response = f.get();
        if (response.getStatusCode() != 200) {
            throw new Exception(response.getResponseBody());
        }
    }

}
