package cc.blynk.server.internal;

import cc.blynk.server.core.protocol.enums.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;

import static cc.blynk.server.core.protocol.enums.Command.DEVICE_OFFLINE;
import static cc.blynk.server.core.protocol.enums.Response.DEVICE_NOT_IN_NETWORK;
import static cc.blynk.server.core.protocol.enums.Response.FACEBOOK_USER_LOGIN_WITH_PASS;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND_BODY;
import static cc.blynk.server.core.protocol.enums.Response.INVALID_TOKEN;
import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_ERROR;
import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_NOT_AUTHORIZED;
import static cc.blynk.server.core.protocol.enums.Response.NOT_ALLOWED;
import static cc.blynk.server.core.protocol.enums.Response.NO_ACTIVE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Response.NO_DATA;
import static cc.blynk.server.core.protocol.enums.Response.OK;
import static cc.blynk.server.core.protocol.enums.Response.SERVER_ERROR;
import static cc.blynk.server.core.protocol.enums.Response.USER_ALREADY_REGISTERED;
import static cc.blynk.server.core.protocol.enums.Response.USER_NOT_AUTHENTICATED;
import static cc.blynk.server.core.protocol.enums.Response.USER_NOT_REGISTERED;
import static cc.blynk.server.core.protocol.model.messages.MessageBase.HEADER_LENGTH;
import static cc.blynk.utils.StringUtils.DEVICE_SEPARATOR;
import static io.netty.buffer.ByteBufAllocator.DEFAULT;

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

    public static ByteBuf notificationError(int msgId) {
        return makeResponse(msgId, NOTIFICATION_ERROR);
    }

    public static ByteBuf deviceNotInNetwork(int msgId) {
        return makeResponse(msgId, DEVICE_NOT_IN_NETWORK);
    }

    public static ByteBuf noActiveDash(int msgId) {
        return makeResponse(msgId, NO_ACTIVE_DASHBOARD);
    }

    public static ByteBuf notAllowed(int msgId) {
        return makeResponse(msgId, NOT_ALLOWED);
    }

    public static ByteBuf illegalCommandBody(int msgId) {
        return makeResponse(msgId, ILLEGAL_COMMAND_BODY);
    }

    public static ByteBuf illegalCommand(int msgId) {
        return makeResponse(msgId, ILLEGAL_COMMAND);
    }

    public static ByteBuf invalidToken(int msgId) {
        return makeResponse(msgId, INVALID_TOKEN);
    }

    public static ByteBuf alreadyRegistered(int msgId) {
        return makeResponse(msgId, USER_ALREADY_REGISTERED);
    }

    public static ByteBuf serverError(int msgId) {
        return makeResponse(msgId, SERVER_ERROR);
    }

    public static ByteBuf noData(int msgId) {
        return makeResponse(msgId, NO_DATA);
    }

    public static ByteBuf ok(int msgId) {
        return makeResponse(msgId, OK);
    }

    public static ByteBuf notRegistered(int msgId) {
        return makeResponse(msgId, USER_NOT_REGISTERED);
    }

    public static ByteBuf facebookUserLoginWithPass(int msgId) {
        return makeResponse(msgId, FACEBOOK_USER_LOGIN_WITH_PASS);
    }

    public static ByteBuf notAuthenticated(int msgId) {
        return makeResponse(msgId, USER_NOT_AUTHENTICATED);
    }

    public static ByteBuf notificationNotAuthorized(int msgId) {
        return makeResponse(msgId, NOTIFICATION_NOT_AUTHORIZED);
    }

    public static ByteBuf deviceOffline(int dashId, int deviceId) {
        return makeASCIIStringMessage(DEVICE_OFFLINE, 0,
                String.valueOf(dashId) + DEVICE_SEPARATOR + deviceId);
    }

    public static ByteBuf makeResponse(int msgId, int responseCode) {
        return DEFAULT.buffer(HEADER_LENGTH)
                .writeByte(Command.RESPONSE)
                .writeShort(msgId)
                .writeShort(responseCode);
    }

    public static ByteBuf makeUTF8StringMessage(short cmd, int msgId, String data) {
        ByteBuf byteBuf = DEFAULT.buffer(HEADER_LENGTH + ByteBufUtil.utf8MaxBytes(data))
                .writeByte(cmd)
                .writeShort(msgId)
                .writerIndex(HEADER_LENGTH);

        int bytesWritten = ByteBufUtil.writeUtf8(byteBuf, data);
        return byteBuf.setShort(3, bytesWritten);
    }

    public static ByteBuf makeASCIIStringMessage(short cmd, int msgId, String data) {
        int dataLength = data.length();
        ByteBuf byteBuf = DEFAULT.buffer(HEADER_LENGTH + dataLength)
                .writeByte(cmd)
                .writeShort(msgId)
                .writeShort(dataLength);

        ByteBufUtil.writeAscii(byteBuf, data);
        return byteBuf;
    }

    public static ByteBuf makeBinaryMessage(short cmd, int msgId, byte[] byteData) {
        int dataLength = byteData.length;
        return ByteBufAllocator.DEFAULT.buffer(HEADER_LENGTH + dataLength)
                .writeByte(cmd)
                .writeShort(msgId)
                .writeShort(dataLength)
                .writeBytes(byteData);
    }

}
