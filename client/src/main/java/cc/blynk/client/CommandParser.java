package cc.blynk.client;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 * Convertor between user-friendly command and protocol command code
 */
public class CommandParser {

    public static Short parseCommand(String stringCommand) {
        switch (stringCommand.toLowerCase()) {
            case "hardware" :
                return HARDWARE;
            case "ping" :
                return PING;
            case "loadprofile" :
                return LOAD_PROFILE;
            case "loadprofilegzipped" :
                return LOAD_PROFILE_GZIPPED;
            case "sync" :
                return SYNC;
            case "saveprofile" :
                return SAVE_PROFILE;
            case "gettoken" :
                return GET_TOKEN;
            case "refreshtoken" :
                return REFRESH_TOKEN;
            case "login" :
                return LOGIN;
            case "getgraphdata" :
                return GET_GRAPH_DATA;
            case "activate" :
                return ACTIVATE_DASHBOARD;
            case "deactivate" :
                return DEACTIVATE_DASHBOARD;
            case "register" :
                return REGISTER;
            case "tweet" :
                return TWEET;
            case "email" :
                return EMAIL;
            case "push" :
                return PUSH_NOTIFICATION;
            case "bridge" :
                return BRIDGE;
            case "createdash" :
                return CREATE_DASH;
            case "savedash" :
                return SAVE_DASH;
            case "deletedash" :
                return DELETE_DASH;

            //sharing section
            case "sharelogin" :
                return SHARE_LOGIN;
            case "getsharetoken" :
                return GET_SHARE_TOKEN;
            case "getshareddash" :
                return GET_SHARED_DASH;
            case "refreshsharetoken" :
                return REFRESH_SHARE_TOKEN;

            default:
                throw new IllegalArgumentException("Unsupported command");
        }
    }

}
