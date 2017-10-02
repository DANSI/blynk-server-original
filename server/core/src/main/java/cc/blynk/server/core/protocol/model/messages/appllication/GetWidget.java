package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.GET_WIDGET;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class GetWidget extends StringMessage {

    public GetWidget(int messageId, String body) {
        super(messageId, GET_WIDGET, body.length(), body);
    }

    @Override
    public String toString() {
        return "GetWidget{" + super.toString() + "}";
    }
}
