package cc.blynk.server.core.processors;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.eventor.Rule;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.SetPinAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.notification.MailAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.notification.NotificationAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.notification.NotifyAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.notification.TwitAction;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import cc.blynk.utils.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.EVENTOR;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.utils.StringUtils.PIN_PATTERN;

/**
 * Class responsible for handling eventor logic.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.08.16.
 */
public class EventorProcessor {

    private static final Logger log = LogManager.getLogger(EventorProcessor.class);

    private final GCMWrapper gcmWrapper;
    private final TwitterWrapper twitterWrapper;
    private final BlockingIOProcessor blockingIOProcessor;
    private final GlobalStats globalStats;

    public EventorProcessor(GCMWrapper gcmWrapper, TwitterWrapper twitterWrapper, BlockingIOProcessor blockingIOProcessor, GlobalStats stats) {
        this.gcmWrapper = gcmWrapper;
        this.twitterWrapper = twitterWrapper;
        this.blockingIOProcessor = blockingIOProcessor;
        this.globalStats = stats;
    }

    public void process(Session session, DashBoard dash, byte pin, PinType type, String triggerValue) {
        Eventor eventor = dash.getWidgetByType(Eventor.class);
        if (eventor == null || eventor.rules == null) {
            return;
        }

        double valueParsed;
        try {
            valueParsed = Double.parseDouble(triggerValue);
        } catch (NumberFormatException nfe) {
            return;
        }

        for (Rule rule : eventor.rules) {
            if (rule.isReady(pin, type)) {
                if (rule.isValid(valueParsed)) {
                    if (!rule.isProcessed) {
                        for (BaseAction action : rule.actions) {
                            if (action.isValid()) {
                                if (action instanceof SetPinAction) {
                                    execute(session, dash, (SetPinAction) action);
                                } else if (action instanceof NotificationAction) {
                                    execute(dash, triggerValue, (NotificationAction) action);
                                }
                            }
                        }
                        rule.isProcessed = true;
                    }
                } else {
                    rule.isProcessed = false;
                }
            }
        }
    }

    private void execute(DashBoard dash, String triggerValue, NotificationAction notificationAction) {
        String body = notificationAction.message.replaceAll(PIN_PATTERN, triggerValue);
        if (notificationAction instanceof NotifyAction) {
            push(dash, body);
        } else if (notificationAction instanceof TwitAction) {
            twit(dash, body);
        } else if (notificationAction instanceof MailAction) {
            //email(dash, body);
        }
        globalStats.mark(EVENTOR);
    }

    private void twit(DashBoard dash, String body) {
        if (Twitter.isWrongBody(body)) {
            log.debug("Wrong twit body.");
            return;
        }

        Twitter twitterWidget = dash.getWidgetByType(Twitter.class);

        if (twitterWidget == null || !dash.isActive ||
                twitterWidget.token == null || twitterWidget.token.equals("") ||
                twitterWidget.secret == null || twitterWidget.secret.equals("")) {
            log.debug("User has no access token provided.");
            return;
        }

        blockingIOProcessor.execute(() -> {
            try {
                twitterWrapper.send(twitterWidget.token, twitterWidget.secret, body);
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Eventor. Status is a duplicate")) {
                    log.error("Error sending twit. Reason : {}", e.getMessage());
                }
            }
        });
    }


    private void push(DashBoard dash, String body) {
        if (Notification.isWrongBody(body)) {
            log.debug("Wrong push body.");
            return;
        }

        if (!dash.isActive) {
            log.debug("Project not active.");
            return;
        }

        Notification widget = dash.getWidgetByType(Notification.class);

        if (widget == null || widget.hasNoToken()) {
            log.debug("User has no access token provided.");
            return;
        }

        widget.push(gcmWrapper, body, dash.id);
    }

    private void execute(Session session, DashBoard dash, SetPinAction action) {
        execute(session, dash.isActive, dash.id, action.pin, action.value);
        dash.update(action.pin.pin, action.pin.pinType, action.value);
    }

    private void execute(Session session, boolean isActive, int dashId, Pin pin, String value) {
        final String body = Pin.makeHardwareBody(pin.pwmMode, pin.pinType, pin.pin, value);
        session.sendMessageToHardware(dashId, HARDWARE, 888, body);
        if (isActive) {
            session.sendToApps(HARDWARE, 888, dashId + StringUtils.BODY_SEPARATOR_STRING + body);
        }
        globalStats.mark(EVENTOR);
    }
}
