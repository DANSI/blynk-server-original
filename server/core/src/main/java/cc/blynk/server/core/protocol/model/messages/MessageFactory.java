package cc.blynk.server.core.protocol.model.messages;

import cc.blynk.server.core.protocol.exceptions.UnsupportedCommandException;
import cc.blynk.server.core.protocol.model.messages.appllication.*;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.*;
import cc.blynk.server.core.protocol.model.messages.common.HardwareConnectedMessage;
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
            case APP_SYNC:
                return new AppSyncMessage(messageId, body);
            case SHARING :
                return new SharingMessage(messageId, body);
            case GET_TOKEN :
                return new GetTokenMessage(messageId, body);
            case ASSIGN_TOKEN :
                return new AssignTokenMessage(messageId, body);
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
            case EXPORT_GRAPH_DATA :
                return new ExportDataMessage(messageId, body);
            case SET_WIDGET_PROPERTY :
                return new SetWidgetPropertyMessage(messageId, body);

            case TWEET :
                return new TwitMessage(messageId, body);
            case EMAIL :
                return new MailMessage(messageId, body);
            case PUSH_NOTIFICATION :
                return new PushMessage(messageId, body);
            case SMS :
                return new SMSMessage(messageId, body);

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
            case HARDWARE_CONNECTED :
                return new HardwareConnectedMessage(messageId, body);
            case REDEEM :
                return new RedeemMessage(messageId, body);

            case CREATE_DASH :
                return new CreateDashMessage(messageId, body);
            case UPDATE_DASH:
                return new UpdateDashMessage(messageId, body);
            case DELETE_DASH :
                return new DeleteDashMessage(messageId, body);
            case UPDATE_PROJECT_SETTINGS :
                return new UpdateDashSettingsMessage(messageId, body);

            case ADD_PUSH_TOKEN :
                return new AddPushToken(messageId, body);
            case HARDWARE_SYNC :
                return new HardwareSyncMessage(messageId, body);
            case BLYNK_INTERNAL:
                return new BlynkInternalMessage(messageId, body);

            case CREATE_WIDGET :
                return new CreateWidget(messageId, body);
            case UPDATE_WIDGET :
                return new UpdateWidget(messageId, body);
            case DELETE_WIDGET :
                return new DeleteWidget(messageId, body);

            case CREATE_DEVICE :
                return new CreateDevice(messageId, body);
            case UPDATE_DEVICE :
                return new UpdateDevice(messageId, body);
            case DELETE_DEVICE :
                return new DeleteDevice(messageId, body);
            case GET_DEVICES :
                return new GetDevices(messageId, body);

            case CREATE_TAG :
                return new CreateTag(messageId, body);
            case UPDATE_TAG :
                return new UpdateTag(messageId, body);
            case DELETE_TAG :
                return new DeleteTag(messageId, body);
            case GET_TAGS :
                return new GetTags(messageId, body);

            case GET_ENERGY :
                return new GetEnergy(messageId, body);
            case ADD_ENERGY :
                return new AddEnergy(messageId, body);

            case GET_SERVER :
                return new GetServerMessage(messageId, body);
            case CONNECT_REDIRECT :
                return new ConnectRedirectMessage(messageId, body);

            case APP_CONNECTED :
                return new AppConnectedMessage(messageId);

            case CREATE_APP :
                return new CreateAppMessage(messageId, body);
            case UPDATE_APP :
                return new UpdateAppMessage(messageId, body);
            case DELETE_APP :
                return new DeleteAppMessage(messageId, body);
            case GET_PROJECT_BY_TOKEN :
                return new GetProjectByTokenStringMessage(messageId, body);
            case EMAIL_QR :
                return new EmailQRsMessage(messageId, body);
            case UPDATE_FACE :
                return new UpdateFaceMessage(messageId, body);

            default: throw new UnsupportedCommandException("Command not supported. Code : " + command, messageId);
        }
    }

}
