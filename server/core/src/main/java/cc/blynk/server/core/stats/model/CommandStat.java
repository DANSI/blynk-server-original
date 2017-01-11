package cc.blynk.server.core.stats.model;

import static cc.blynk.server.core.protocol.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 11.01.17.
 */
public class CommandStat {

    public long response;
    public long redeem;
    public long hardwareConnected;
    public long register;
    public long login;
    public long loadProfile;
    public long appSync;
    public long sharing;
    public long getToken;
    public long ping;
    public long activate;
    public long deactivate;
    public long refreshToken;
    public long getGraphData;
    public long exportGraphData;
    public long setWidgetProperty;
    public long bridge;
    public long hardware;
    public long getSharedDash;
    public long getShareToken;
    public long refreshShareToken;
    public long shareLogin;
    public long createProject;
    public long updateProject;
    public long deleteProject;
    public long hardwareSync;
    public long internal;

    public long sms;
    public long tweet;
    public long email;
    public long push;
    public long addPushToken;

    public long createWidget;
    public long updateWidget;
    public long deleteWidget;

    public long createDevice;
    public long updateDevice;
    public long deleteDevice;
    public long getDevices;

    public long addEnergy;
    public long getEnergy;

    public long getServer;
    public long connectRedirect;

    public long webSockets;

    public long eventor;
    public long webhooks;

    public void assign(short field, long val) {
        switch (field) {
            case RESPONSE :
                this.response = val;
                break;
            case REDEEM :
                this.redeem = val;
                break;
            case HARDWARE_CONNECTED :
                this.hardwareConnected = val;
                break;
            case REGISTER :
                this.register = val;
                break;
            case LOGIN :
                this.login = val;
                break;
            case LOAD_PROFILE_GZIPPED :
                this.loadProfile = val;
                break;
            case APP_SYNC :
                this.appSync = val;
                break;
            case SHARING :
                this.sharing = val;
                break;
            case GET_TOKEN :
                this.getToken = val;
                break;
            case PING :
                this.ping = val;
                break;
            case SMS :
                this.sms = val;
                break;
            case ACTIVATE_DASHBOARD :
                this.activate = val;
                break;
            case DEACTIVATE_DASHBOARD :
                this.deactivate = val;
                break;
            case REFRESH_TOKEN :
                this.refreshToken = val;
                break;
            case GET_GRAPH_DATA :
                this.getGraphData = val;
                break;
            case EXPORT_GRAPH_DATA :
                this.exportGraphData = val;
                break;
            case SET_WIDGET_PROPERTY :
                this.setWidgetProperty = val;
                break;
            case BRIDGE :
                this.bridge = val;
                break;
            case HARDWARE :
                this.hardware = val;
                break;
            case GET_SHARED_DASH :
                this.getSharedDash = val;
                break;
            case GET_SHARE_TOKEN :
                this.getShareToken = val;
                break;
            case REFRESH_SHARE_TOKEN :
                this.refreshShareToken = val;
                break;
            case SHARE_LOGIN :
                this.shareLogin = val;
                break;
            case CREATE_DASH :
                this.createProject = val;
                break;
            case UPDATE_DASH :
                this.updateProject = val;
                break;
            case DELETE_DASH :
                this.deleteProject = val;
                break;
            case HARDWARE_SYNC :
                this.hardwareSync = val;
                break;
            case BLYNK_INTERNAL :
                this.internal = val;
                break;
            case ADD_PUSH_TOKEN :
                this.addPushToken = val;
                break;
            case TWEET :
                this.tweet = val;
                break;
            case EMAIL :
                this.email = val;
                break;
            case PUSH_NOTIFICATION :
                this.push = val;
                break;
            case CREATE_WIDGET :
                this.createWidget = val;
                break;
            case UPDATE_WIDGET :
                this.updateWidget = val;
                break;
            case DELETE_WIDGET :
                this.deleteWidget = val;
                break;
            case CREATE_DEVICE :
                this.createDevice = val;
                break;
            case UPDATE_DEVICE :
                this.updateDevice = val;
                break;
            case DELETE_DEVICE :
                this.deleteDevice = val;
                break;
            case GET_DEVICES :
                this.getDevices = val;
                break;
            case ADD_ENERGY :
                this.addEnergy = val;
                break;
            case GET_ENERGY :
                this.getEnergy = val;
                break;
            case GET_SERVER :
                this.getServer = val;
                break;
            case CONNECT_REDIRECT :
                this.connectRedirect = val;
                break;
            case WEB_SOCKETS :
                this.webSockets = val;
                break;
            case EVENTOR :
                this.eventor = val;
                break;
            case WEB_HOOKS :
                this.webhooks = val;
                break;
        }
    }

}
