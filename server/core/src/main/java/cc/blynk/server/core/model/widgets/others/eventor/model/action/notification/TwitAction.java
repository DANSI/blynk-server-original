package cc.blynk.server.core.model.widgets.others.eventor.model.action.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class TwitAction extends NotificationAction {

    @JsonCreator
    public TwitAction(@JsonProperty("message") String message) {
        super(message);
    }

}
