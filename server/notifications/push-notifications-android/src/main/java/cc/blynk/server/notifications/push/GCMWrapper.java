package cc.blynk.server.notifications.push;

import cc.blynk.common.utils.ServerProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jivesoftware.smack.SmackException;

import java.util.HashMap;
import java.util.Map;

import static cc.blynk.server.notifications.push.GCMSmackCcsClient.createRequest;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/8/2015.
 */
public class GCMWrapper {

    private static final Logger log = LogManager.getLogger(GCMSmackCcsClient.class);

    private static final String filePropertiesName = "gcm.properties";
    private final GCMSmackCcsClient ccsClient;

    public GCMWrapper() {
        ServerProperties props = new ServerProperties(filePropertiesName);

        this.ccsClient = new GCMSmackCcsClient(props.getProperty("gcm.server"), props.getIntProperty("gcm.port"));

        try {
            ccsClient.connect(props.getLongProperty("gcm.project.id"), props.getProperty("gcm.api.key"));
        } catch (Exception e) {
            log.error("Error connecting to google push server.", e);
        }
    }

    public void send(String toRegId, String payload)
            throws SmackException.NotConnectedException, JsonProcessingException {
        String messageId = GCMSmackCcsClient.generateUniqueMesageId();
        long timeToLive = 86400;

        Map<String, String> data = new HashMap<String, String>() {
            {
                put("message", payload);
            }
        };

        String message = createRequest(toRegId, messageId, data, timeToLive, true);
        ccsClient.sendDownstreamMessage(message);
    }

}
