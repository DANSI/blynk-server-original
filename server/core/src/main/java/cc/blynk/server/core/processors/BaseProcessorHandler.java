package cc.blynk.server.core.processors;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.protocol.exceptions.QuotaLimitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple abstract class for handling all processor engines.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.08.17.
 */
public abstract class BaseProcessorHandler {

    protected static final Logger log = LogManager.getLogger(BaseProcessorHandler.class);

    private final EventorProcessor eventorProcessor;
    private final WebhookProcessor webhookProcessor;

    protected BaseProcessorHandler(EventorProcessor eventorProcessor, WebhookProcessor webhookProcessor) {
        this.eventorProcessor = eventorProcessor;
        this.webhookProcessor = webhookProcessor;
    }

    protected void processEventorAndWebhook(User user, DashBoard dash, int deviceId, Session session, short pin,
                                            PinType pinType, String value, long now) {
        try {
            eventorProcessor.process(user, session, dash, deviceId, pin, pinType, value, now);
            webhookProcessor.process(user, session, dash, deviceId, pin, pinType, value, now);
        } catch (QuotaLimitException qle) {
            log.debug("User {} reached notification limit for eventor/webhook.", user.name);
        } catch (IllegalArgumentException iae) {
            log.debug("Error processing webhook for {}. Reason : {}", user.email, iae.getMessage());
        } catch (Exception e) {
            log.error("Error processing eventor/webhook.", e);
        }
    }

}
