package cc.blynk.server.core.protocol.model.messages;

import cc.blynk.server.core.protocol.exceptions.UnsupportedCommandException;
import cc.blynk.server.core.protocol.model.messages.appllication.*;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.*;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.core.protocol.model.messages.common.PingMessage;
import cc.blynk.server.core.protocol.model.messages.hardware.*;

import static cc.blynk.server.core.protocol.enums.Command.*;

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
            case LOAD_PROFILE_GZIPPED :
                return new LoadProfileGzippedStringMessage(messageId, body);
            case SYNC :
                return new SyncMessage(messageId, body);
            case SHARING :
                return new SharingMessage(messageId, body);
            case GET_TOKEN :
                return new GetTokenMessage(messageId, body);
            case PING :
                return new PingMessage(messageId);
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
            case ADD_PUSH_TOKEN :
                return new AddPushToken(messageId, body);
            case SAVE_DASH :
                return new SaveDashMessage(messageId, body);
            case HARDWARE_SYNC :
                return new HardwareSyncMessage(messageId, body);
            case HARDWARE_INFO :
                return new HardwareInfoMessage(messageId, body);

            default: throw new UnsupportedCommandException(String.format("Command with code %d not supported message.", command), messageId);
        }
    }

}
