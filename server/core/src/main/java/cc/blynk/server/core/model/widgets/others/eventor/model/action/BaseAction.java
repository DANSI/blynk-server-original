package cc.blynk.server.core.model.widgets.others.eventor.model.action;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SetPin.class, name = "SETPIN"),
        @JsonSubTypes.Type(value = Wait.class, name = "WAIT"),
        @JsonSubTypes.Type(value = Notify.class, name = "NOTIFY"),
        @JsonSubTypes.Type(value = Mail.class, name = "MAIL"),
        @JsonSubTypes.Type(value = Twit.class, name = "TWIT"),
})
public abstract class BaseAction {

    public abstract void execute(ChannelHandlerContext ctx, String triggerValue);

    public String format(String message, String triggerValue) {
        return message.replaceAll("/pin/", triggerValue);
    }
}
