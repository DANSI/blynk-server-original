package cc.blynk.client;

import static cc.blynk.server.core.protocol.enums.Command.ACTIVATE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Command.ADD_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.ADD_PUSH_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.ASSIGN_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.BLYNK_INTERNAL;
import static cc.blynk.server.core.protocol.enums.Command.BRIDGE;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_APP;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_DASH;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_REPORT;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_TILE_TEMPLATE;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.DEACTIVATE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_APP;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_DASH;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_DEVICE_DATA;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_ENHANCED_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_REPORT;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_TILE_TEMPLATE;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.EMAIL;
import static cc.blynk.server.core.protocol.enums.Command.EMAIL_QR;
import static cc.blynk.server.core.protocol.enums.Command.EXPORT_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.EXPORT_REPORT;
import static cc.blynk.server.core.protocol.enums.Command.GET_CLONE_CODE;
import static cc.blynk.server.core.protocol.enums.Command.GET_DEVICES;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENHANCED_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_CLONE_CODE;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROVISION_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_SERVER;
import static cc.blynk.server.core.protocol.enums.Command.GET_SHARE_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_TAGS;
import static cc.blynk.server.core.protocol.enums.Command.GET_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_LOGIN;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_RESEND_FROM_BLUETOOTH;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.server.core.protocol.enums.Command.LOGIN;
import static cc.blynk.server.core.protocol.enums.Command.LOGOUT;
import static cc.blynk.server.core.protocol.enums.Command.MOBILE_GET_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.PING;
import static cc.blynk.server.core.protocol.enums.Command.PUSH_NOTIFICATION;
import static cc.blynk.server.core.protocol.enums.Command.REFRESH_SHARE_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.REFRESH_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.REGISTER;
import static cc.blynk.server.core.protocol.enums.Command.RESET_PASSWORD;
import static cc.blynk.server.core.protocol.enums.Command.RESOLVE_EVENT;
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
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_REPORT;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_TILE_TEMPLATE;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_WIDGET;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 * Convertor between user-friendly command and protocol command code
 */
public final class CommandParserUtil {

    private CommandParserUtil() {
    }

    public static Short parseCommand(String stringCommand) {
        switch (stringCommand.toLowerCase()) {
            case "hardware" :
                return HARDWARE;
            case "hardwarebt" :
                return HARDWARE_RESEND_FROM_BLUETOOTH;
            case "ping" :
                return PING;
            case "loadprofilegzipped" :
                return LOAD_PROFILE_GZIPPED;
            case "appsync" :
                return APP_SYNC;
            case "sharing" :
                return SHARING;
            case "assigntoken" :
                return ASSIGN_TOKEN;
            case "refreshtoken" :
                return REFRESH_TOKEN;
            case "login" :
                return LOGIN;
            case "hardwarelogin" :
                return HARDWARE_LOGIN;
            case "logout" :
                return LOGOUT;
            case "getenhanceddata" :
                return GET_ENHANCED_GRAPH_DATA;
            case "deleteenhanceddata" :
                return DELETE_ENHANCED_GRAPH_DATA;
            case "export" :
                return EXPORT_GRAPH_DATA;
            case "activate" :
                return ACTIVATE_DASHBOARD;
            case "deactivate" :
                return DEACTIVATE_DASHBOARD;
            case "register" :
                return REGISTER;
            case "setproperty" :
                return SET_WIDGET_PROPERTY;

            case "tweet" :
                return TWEET;
            case "email" :
                return EMAIL;
            case "push" :
                return PUSH_NOTIFICATION;
            case "sms" :
                return SMS;
            case "addpushtoken" :
                return ADD_PUSH_TOKEN;

            case "bridge" :
                return BRIDGE;

            case "createdash" :
                return CREATE_DASH;
            case "updatedash" :
                return UPDATE_DASH;
            case "deletedash" :
                return DELETE_DASH;
            case "updatesettings" :
                return UPDATE_PROJECT_SETTINGS;

            case "createwidget" :
                return CREATE_WIDGET;
            case "updatewidget" :
                return UPDATE_WIDGET;
            case "deletewidget" :
                return DELETE_WIDGET;
            case "getwidget" :
                return GET_WIDGET;

            case "hardsync" :
                return HARDWARE_SYNC;
            case "internal" :
                return BLYNK_INTERNAL;

            case "createtemplate" :
                return CREATE_TILE_TEMPLATE;
            case "updatetemplate" :
                return UPDATE_TILE_TEMPLATE;
            case "deletetemplate" :
                return DELETE_TILE_TEMPLATE;

            case "createdevice" :
                return CREATE_DEVICE;
            case "updatedevice" :
                return UPDATE_DEVICE;
            case "deletedevice" :
                return DELETE_DEVICE;
            case "getdevices" :
                return GET_DEVICES;
            case "getdevice" :
                return MOBILE_GET_DEVICE;

            case "createtag" :
                return CREATE_TAG;
            case "updatetag" :
                return UPDATE_TAG;
            case "deletetag" :
                return DELETE_TAG;
            case "gettags" :
                return GET_TAGS;

            case "addenergy" :
                return ADD_ENERGY;
            case "getenergy" :
                return GET_ENERGY;

            case "getserver" :
                return GET_SERVER;

            //sharing section
            case "sharelogin" :
                return SHARE_LOGIN;
            case "getsharetoken" :
                return GET_SHARE_TOKEN;
            case "refreshsharetoken" :
                return REFRESH_SHARE_TOKEN;

            case "createapp" :
                return CREATE_APP;
            case "updateapp" :
                return UPDATE_APP;
            case "deleteapp" :
                return DELETE_APP;
            case "getprojectbytoken" :
                return GET_PROJECT_BY_TOKEN;
            case "emailqr" :
                return EMAIL_QR;
            case "updateface" :
                return UPDATE_FACE;
            case "getclonecode" :
                return GET_CLONE_CODE;
            case "getprojectbyclonecode" :
                return GET_PROJECT_BY_CLONE_CODE;
            case "getprovisiontoken" :
                return GET_PROVISION_TOKEN;
            case "resolveevent" :
                return RESOLVE_EVENT;
            case "deletedevicedata" :
                return DELETE_DEVICE_DATA;

            case "createreport" :
                return CREATE_REPORT;
            case "deletereport" :
                return DELETE_REPORT;
            case "updatereport" :
                return UPDATE_REPORT;
            case "exportreport" :
                return EXPORT_REPORT;
            case "resetpass" :
                return RESET_PASSWORD;

            default:
                throw new IllegalArgumentException("Unsupported command");
        }
    }

}
