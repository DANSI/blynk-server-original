package cc.blynk.common.model.messages;

import cc.blynk.common.enums.Command;
import cc.blynk.common.exceptions.BaseServerException;
import cc.blynk.common.exceptions.UnsupportedCommandException;
import cc.blynk.common.model.messages.protocol.BridgeMessage;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.model.messages.protocol.PingMessage;
import cc.blynk.common.model.messages.protocol.appllication.*;
import cc.blynk.common.model.messages.protocol.appllication.sharing.*;
import cc.blynk.common.model.messages.protocol.hardware.MailMessage;
import cc.blynk.common.model.messages.protocol.hardware.PushMessage;
import cc.blynk.common.model.messages.protocol.hardware.TweetMessage;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class MessageFactory {

    public static MessageBase produce(int messageId, short command, String body) {
        switch (command) {
            case REGISTER :
                return new RegisterMessage(messageId, body);
            case LOGIN :
                return new LoginMessage(messageId, body);
            case SAVE_PROFILE :
                return new SaveProfileMessage(messageId, body);
            case LOAD_PROFILE :
                return new LoadProfileMessage(messageId, body);
            case LOAD_PROFILE_GZIPPED :
                return new LoadProfileGzippedStringMessage(messageId, body);
            case SYNC :
                return new SyncMessage(messageId, body);
            case GET_TOKEN :
                return new GetTokenMessage(messageId, body);
            case PING :
                return new PingMessage(messageId, body);
            case ACTIVATE_DASHBOARD :
                return new ActivateDashboardMessage(messageId, body);
            case DEACTIVATE_DASHBOARD :
                return new DeActivateDashboardMessage(messageId, body);
            case REFRESH_TOKEN :
                return new RefreshTokenMessage(messageId, body);
            case GET_GRAPH_DATA :
                return new GetGraphDataStringMessage(messageId, body);
            case TWEET :
                return new TweetMessage(messageId, body);
            case EMAIL :
                return new MailMessage(messageId, body);
            case PUSH_NOTIFICATION :
                return new PushMessage(messageId, body);
            case BRIDGE :
                return new BridgeMessage(messageId, body);
            case SHARE_LOGIN :
                return new ShareLoginMessage(messageId, body);
            case GET_SHARE_TOKEN :
                return new GetShareTokenMessage(messageId, body);
            case REFRESH_SHARE_TOKEN :
                return new RefreshShareTokenMessage(messageId, body);
            case GET_SHARED_DASH :
                return new GetSharedDashMessage(messageId, body);
            case HARDWARE :
                return new HardwareMessage(messageId, body);
            case CREATE_DASH :
                return new CreateDashMessage(messageId, body);
            case DELETE_DASH :
                return new DeleteDashMessage(messageId, body);
            case SAVE_DASH :
                return new SaveDashMessage(messageId, body);

            default: throw new UnsupportedCommandException(String.format("Command with code %d not supported message.", command), messageId);
        }
    }

    public static ResponseMessage produce(int messageId, int responseCode) {
        return new ResponseMessage(messageId, Command.RESPONSE, responseCode);
    }

    public static ResponseMessage produce(BaseServerException exception) {
        return new ResponseMessage(exception.msgId, Command.RESPONSE, exception.errorCode);
    }

}
