package cc.blynk.server.internal;

import cc.blynk.server.core.protocol.model.messages.BinaryMessage;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;

import java.nio.charset.StandardCharsets;

import static cc.blynk.server.core.protocol.enums.Command.DEVICE_OFFLINE;
import static cc.blynk.server.core.protocol.enums.Response.DEVICE_NOT_IN_NETWORK;
import static cc.blynk.server.core.protocol.enums.Response.ENERGY_LIMIT;
import static cc.blynk.server.core.protocol.enums.Response.FACEBOOK_USER_LOGIN_WITH_PASS;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND_BODY;
import static cc.blynk.server.core.protocol.enums.Response.INVALID_TOKEN;
import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_ERROR;
import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_INVALID_BODY;
import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_NOT_AUTHORIZED;
import static cc.blynk.server.core.protocol.enums.Response.NOT_ALLOWED;
import static cc.blynk.server.core.protocol.enums.Response.NO_ACTIVE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Response.NO_DATA;
import static cc.blynk.server.core.protocol.enums.Response.OK;
import static cc.blynk.server.core.protocol.enums.Response.QUOTA_LIMIT;
import static cc.blynk.server.core.protocol.enums.Response.SERVER_ERROR;
import static cc.blynk.server.core.protocol.enums.Response.USER_ALREADY_REGISTERED;
import static cc.blynk.server.core.protocol.enums.Response.USER_NOT_AUTHENTICATED;
import static cc.blynk.server.core.protocol.enums.Response.USER_NOT_REGISTERED;
import static cc.blynk.utils.StringUtils.DEVICE_SEPARATOR;

/**
 * Utility class that creates native netty buffers instead of java objects.
 * This is done in order to allocate less java objects and reduce GC pauses and load.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.03.16.
 */
public final class CommonByteBufUtil {

    private CommonByteBufUtil() {
    }

    public static ResponseMessage energyLimit(int msgId) {
        return makeResponse(msgId, ENERGY_LIMIT);
    }

    public static ResponseMessage notificationInvalidBody(int msgId) {
        return makeResponse(msgId, NOTIFICATION_INVALID_BODY);
    }

    public static ResponseMessage notificationError(int msgId) {
        return makeResponse(msgId, NOTIFICATION_ERROR);
    }

    public static ResponseMessage deviceNotInNetwork(int msgId) {
        return makeResponse(msgId, DEVICE_NOT_IN_NETWORK);
    }

    public static ResponseMessage noActiveDash(int msgId) {
        return makeResponse(msgId, NO_ACTIVE_DASHBOARD);
    }

    public static ResponseMessage notAllowed(int msgId) {
        return makeResponse(msgId, NOT_ALLOWED);
    }

    public static ResponseMessage illegalCommandBody(int msgId) {
        return makeResponse(msgId, ILLEGAL_COMMAND_BODY);
    }

    public static ResponseMessage illegalCommand(int msgId) {
        return makeResponse(msgId, ILLEGAL_COMMAND);
    }

    public static ResponseMessage invalidToken(int msgId) {
        return makeResponse(msgId, INVALID_TOKEN);
    }

    public static ResponseMessage alreadyRegistered(int msgId) {
        return makeResponse(msgId, USER_ALREADY_REGISTERED);
    }

    public static ResponseMessage serverError(int msgId) {
        return makeResponse(msgId, SERVER_ERROR);
    }

    public static ResponseMessage quotaLimit(int msgId) {
        return makeResponse(msgId, QUOTA_LIMIT);
    }

    public static ResponseMessage noData(int msgId) {
        return makeResponse(msgId, NO_DATA);
    }

    public static ResponseMessage ok(int msgId) {
        return makeResponse(msgId, OK);
    }

    public static ResponseMessage notRegistered(int msgId) {
        return makeResponse(msgId, USER_NOT_REGISTERED);
    }

    public static ResponseMessage facebookUserLoginWithPass(int msgId) {
        return makeResponse(msgId, FACEBOOK_USER_LOGIN_WITH_PASS);
    }

    public static ResponseMessage notAuthenticated(int msgId) {
        return makeResponse(msgId, USER_NOT_AUTHENTICATED);
    }

    public static ResponseMessage notificationNotAuthorized(int msgId) {
        return makeResponse(msgId, NOTIFICATION_NOT_AUTHORIZED);
    }

    public static ResponseMessage makeResponse(int msgId, int responseCode) {
        return new ResponseMessage(msgId, responseCode);
    }

    public static StringMessage deviceOffline(int dashId, int deviceId) {
        return makeASCIIStringMessage(DEVICE_OFFLINE, 0,
                String.valueOf(dashId) + DEVICE_SEPARATOR + deviceId);
    }

    public static StringMessage makeUTF8StringMessage(short cmd, int msgId, String data) {
        return new StringMessage(msgId, cmd, data);
    }

    public static StringMessage makeASCIIStringMessage(short cmd, int msgId, String data) {
        return new StringMessage(msgId, cmd, data, StandardCharsets.US_ASCII);
    }

    public static BinaryMessage makeBinaryMessage(short cmd, int msgId, byte[] byteData) {
        return new BinaryMessage(msgId, cmd, byteData);
    }

}
