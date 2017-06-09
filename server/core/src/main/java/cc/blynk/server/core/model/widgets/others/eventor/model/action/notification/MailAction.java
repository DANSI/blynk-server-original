package cc.blynk.server.core.model.widgets.others.eventor.model.action.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class MailAction extends NotificationAction {

    public final String subject;

    @JsonCreator
    public MailAction(@JsonProperty("subject") String subject,
                      @JsonProperty("message") String message) {
        super(message);
        this.subject = subject;
    }

}
