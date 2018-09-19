package cc.blynk.server.core.protocol.model.messages;

import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.exceptions.UnsupportedCommandException;
import cc.blynk.server.core.protocol.model.messages.appllication.GetServerMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.LoginMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.RegisterMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.ResetPasswordMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.ShareLoginMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.core.protocol.model.messages.hardware.HardwareLoginMessage;

import static cc.blynk.server.core.protocol.enums.Command.GET_SERVER;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_LOGIN;
import static cc.blynk.server.core.protocol.enums.Command.LOGIN;
import static cc.blynk.server.core.protocol.enums.Command.REGISTER;
import static cc.blynk.server.core.protocol.enums.Command.RESET_PASSWORD;
import static cc.blynk.server.core.protocol.enums.Command.SHARE_LOGIN;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public final class MessageFactory {

    private MessageFactory() {
    }

    public static MessageBase produce(int messageId, short commandId, String body) {
        switch (commandId) {
            case REGISTER :
                return new RegisterMessage(messageId, body);
            case LOGIN :
                return new LoginMessage(messageId, body);
            case HARDWARE_LOGIN :
                return new HardwareLoginMessage(messageId, body);
            case SHARE_LOGIN :
                return new ShareLoginMessage(messageId, body);
            case HARDWARE :
                return new HardwareMessage(messageId, body);
            case GET_SERVER :
                return new GetServerMessage(messageId, body);
            case RESET_PASSWORD :
                return new ResetPasswordMessage(messageId, body);
            default:
                if (commandId < Command.LAST_COMMAND_INDEX) {
                    return new StringMessage(messageId, commandId, body);
                }
                throw new UnsupportedCommandException("Command not supported. Code : " + commandId, messageId);
        }
    }

}
