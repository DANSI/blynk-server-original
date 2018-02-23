package cc.blynk.server.core.protocol.model.messages;

import cc.blynk.server.core.protocol.exceptions.UnsupportedCommandException;
import cc.blynk.server.core.protocol.model.messages.appllication.GetServerMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.LoginMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.RegisterMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.ShareLoginMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.core.protocol.model.messages.web.WebAppHardwareMessage;

import static cc.blynk.server.core.protocol.enums.Command.ACTIVATE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Command.ADD_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.ADD_PUSH_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.APP_CONNECTED;
import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.ASSIGN_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.BLYNK_INTERNAL;
import static cc.blynk.server.core.protocol.enums.Command.BRIDGE;
import static cc.blynk.server.core.protocol.enums.Command.CONNECT_REDIRECT;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_APP;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_DASH;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_TILE_TEMPLATE;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.DEACTIVATE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_APP;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_DASH;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_ENHANCED_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_TILE_TEMPLATE;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.DEVICE_OFFLINE;
import static cc.blynk.server.core.protocol.enums.Command.EMAIL;
import static cc.blynk.server.core.protocol.enums.Command.EMAIL_QR;
import static cc.blynk.server.core.protocol.enums.Command.EXPORT_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.GET_CLONE_CODE;
import static cc.blynk.server.core.protocol.enums.Command.GET_DEVICES;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENHANCED_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.GET_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_CLONE_CODE;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_SERVER;
import static cc.blynk.server.core.protocol.enums.Command.GET_SHARE_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_TAGS;
import static cc.blynk.server.core.protocol.enums.Command.GET_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_CONNECTED;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_RESEND_FROM_BLUETOOTH;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.server.core.protocol.enums.Command.LOGIN;
import static cc.blynk.server.core.protocol.enums.Command.LOGOUT;
import static cc.blynk.server.core.protocol.enums.Command.OUTDATED_APP_NOTIFICATION;
import static cc.blynk.server.core.protocol.enums.Command.PING;
import static cc.blynk.server.core.protocol.enums.Command.PUSH_NOTIFICATION;
import static cc.blynk.server.core.protocol.enums.Command.REDEEM;
import static cc.blynk.server.core.protocol.enums.Command.REFRESH_SHARE_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.REFRESH_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.REGISTER;
import static cc.blynk.server.core.protocol.enums.Command.SET_WIDGET_PROPERTY;
import static cc.blynk.server.core.protocol.enums.Command.SHARE_LOGIN;
import static cc.blynk.server.core.protocol.enums.Command.SHARING;
import static cc.blynk.server.core.protocol.enums.Command.SMS;
import static cc.blynk.server.core.protocol.enums.Command.TWEET;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_APP;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_DASH;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_FACE;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_PROJECT_SETTINGS;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_TILE_TEMPLATE;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.WEBAPP_HARDWARE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public final class MessageFactory {

    private MessageFactory() {
    }

    public static MessageBase produce(int messageId, short command, String body) {
        switch (command) {
            case REGISTER :
                return new RegisterMessage(messageId, body);
            case LOGIN :
                return new LoginMessage(messageId, body);
            case SHARE_LOGIN :
                return new ShareLoginMessage(messageId, body);
            case HARDWARE :
                return new HardwareMessage(messageId, body);
            case GET_SERVER :
                return new GetServerMessage(messageId, body);
            case WEBAPP_HARDWARE :
                return new WebAppHardwareMessage(messageId, body);
            case APP_CONNECTED :
            case PING :
            case LOGOUT :
            case LOAD_PROFILE_GZIPPED :
            case APP_SYNC:
            case SHARING :
            case GET_TOKEN :
            case ASSIGN_TOKEN :
            case ACTIVATE_DASHBOARD :
            case DEACTIVATE_DASHBOARD :
            case REFRESH_TOKEN :
            case GET_GRAPH_DATA :
            case GET_ENHANCED_GRAPH_DATA :
            case DELETE_ENHANCED_GRAPH_DATA :
            case EXPORT_GRAPH_DATA :
            case SET_WIDGET_PROPERTY :
            case TWEET :
            case EMAIL :
            case PUSH_NOTIFICATION :
            case SMS :
            case BRIDGE :
            case GET_SHARE_TOKEN :
            case REFRESH_SHARE_TOKEN :
            case HARDWARE_RESEND_FROM_BLUETOOTH :
            case HARDWARE_CONNECTED :
            case REDEEM :
            case CREATE_DASH :
            case UPDATE_DASH :
            case DELETE_DASH :
            case UPDATE_PROJECT_SETTINGS :
            case ADD_PUSH_TOKEN :
            case HARDWARE_SYNC :
            case BLYNK_INTERNAL :
            case CREATE_WIDGET :
            case UPDATE_WIDGET :
            case DELETE_WIDGET :
            case GET_WIDGET :
            case CREATE_TILE_TEMPLATE :
            case UPDATE_TILE_TEMPLATE :
            case DELETE_TILE_TEMPLATE :
            case CREATE_DEVICE :
            case UPDATE_DEVICE :
            case DELETE_DEVICE :
            case GET_DEVICES :
            case CREATE_TAG :
            case UPDATE_TAG :
            case DELETE_TAG :
            case GET_TAGS :
            case GET_ENERGY :
            case ADD_ENERGY :
            case CONNECT_REDIRECT :
            case CREATE_APP :
            case UPDATE_APP :
            case DELETE_APP :
            case GET_PROJECT_BY_TOKEN :
            case EMAIL_QR :
            case UPDATE_FACE :
            case GET_CLONE_CODE :
            case GET_PROJECT_BY_CLONE_CODE :
            case DEVICE_OFFLINE :
            case OUTDATED_APP_NOTIFICATION :
                return new StringMessage(messageId, command, body);

            default: throw new UnsupportedCommandException("Command not supported. Code : " + command, messageId);
        }
    }

}
