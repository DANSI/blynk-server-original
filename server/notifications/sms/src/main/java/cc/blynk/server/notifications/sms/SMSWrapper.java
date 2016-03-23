package cc.blynk.server.notifications.sms;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.03.16.
 */
public class SMSWrapper {

    public static final String SMS_PROPERTIES_FILENAME = "sms.properties";

    private final String key;
    private final String secret;

    private final ObjectReader smsResponseReader = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .reader(SmsResponse.class);

    public SMSWrapper(Properties props) {
        this(props.getProperty("nexmo.api.key"), props.getProperty("nexmo.api.secret"));
    }

    protected SMSWrapper(String key, String secret) {
        this.key = key;
        this.secret = secret;
    }

    public void send(String to, String message) throws Exception {
        URI apiUrl = new URIBuilder()
                .setScheme("https")
                .setHost("rest.nexmo.com")
                .setPath("/sms/json")
                .setParameter("api_key", key)
                .setParameter("api_secret", secret)
                .setParameter("from", "Blynk")
                .setParameter("to", to)
                .setParameter("text", message)
                .build();

        SmsResponse smsResponse = smsResponseReader.readValue(
                Request.Post(apiUrl).execute().returnContent().asString()
        );

        if (!smsResponse.messages[0].status.equals("0")) {
            throw new Exception(smsResponse.messages[0].error);
        }
    }

}
