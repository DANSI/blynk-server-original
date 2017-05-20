package cc.blynk.utils;

import cc.blynk.server.core.protocol.enums.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.server.core.protocol.model.messages.MessageBase.HEADER_LENGTH;

/**
 * Utility class that creates native netty buffers instead of java objects.
 * This is done in order to allocate less java objects and reduce GC pauses and load.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.03.16.
 */
public class BlynkByteBufUtil {

    public static final ByteBufAllocator ALLOCATOR = ByteBufAllocator.DEFAULT;

    public static ByteBuf notificationError(final int msgId) {
        return makeResponse(msgId, NOTIFICATION_ERROR);
    }

    public static ByteBuf deviceNotInNetwork(final int msgId) {
        return makeResponse(msgId, DEVICE_NOT_IN_NETWORK);
    }

    public static ByteBuf noActiveDash(final int msgId) {
        return makeResponse(msgId, NO_ACTIVE_DASHBOARD);
    }

    public static ByteBuf notAllowed(final int msgId) {
        return makeResponse(msgId, NOT_ALLOWED);
    }

    public static ByteBuf illegalCommandBody(final int msgId) {
        return makeResponse(msgId, ILLEGAL_COMMAND_BODY);
    }

    public static ByteBuf illegalCommand(final int msgId) {
        return makeResponse(msgId, ILLEGAL_COMMAND);
    }

    public static ByteBuf ok(final int msgId) {
        return makeResponse(msgId, OK);
    }

    public static ByteBuf makeResponse(final int msgId, final int responseCode) {
        return ALLOCATOR.buffer(HEADER_LENGTH)
                .writeByte(Command.RESPONSE)
                .writeShort(msgId)
                .writeShort(responseCode);
    }

    public static ByteBuf makeUTF8StringMessage(final short cmd, final int msgId, final String data) {
        final ByteBuf byteBuf = ALLOCATOR.buffer(HEADER_LENGTH + ByteBufUtil.utf8MaxBytes(data))
                .writeByte(cmd)
                .writeShort(msgId)
                .writerIndex(HEADER_LENGTH);

        final int bytesWritten = ByteBufUtil.writeUtf8(byteBuf, data);
        return byteBuf.setShort(3, bytesWritten);
    }

    public static ByteBuf makeASCIIStringMessage(final short cmd, final int msgId, final String data) {
        final int dataLength = data.length();
        final ByteBuf byteBuf = ALLOCATOR.buffer(HEADER_LENGTH + dataLength)
                .writeByte(cmd)
                .writeShort(msgId)
                .writeShort(dataLength);

        ByteBufUtil.writeAscii(byteBuf, data);
        return byteBuf;
    }

    public static ByteBuf makeBinaryMessage(final short cmd, final int msgId, final byte[] byteData) {
        final int dataLength = byteData.length;
        return ALLOCATOR.buffer(HEADER_LENGTH + dataLength)
                .writeByte(cmd)
                .writeShort(msgId)
                .writeShort(dataLength)
                .writeBytes(byteData);
    }

}
