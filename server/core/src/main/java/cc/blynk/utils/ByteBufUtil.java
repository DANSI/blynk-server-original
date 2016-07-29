package cc.blynk.utils;

import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.CharsetUtil;

import static cc.blynk.server.core.protocol.enums.Response.*;

/**
 * Utility class that creates native netty buffers instead of java objects.
 * This is done in order to allocate less java objects and reduce GC pauses and load.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.03.16.
 */
public class ByteBufUtil {

    public static ByteBuf ok(int msgId) {
        return makeResponse(msgId, OK);
    }

    public static ByteBuf makeResponse(int msgId, int responseCode) {
        return PooledByteBufAllocator.DEFAULT.buffer(MessageBase.HEADER_LENGTH)
                .writeByte(Command.RESPONSE)
                .writeShort(msgId)
                .writeShort(responseCode);
    }

    public static ByteBuf makeStringMessage(short cmd, int msgId, String data) {
        return makeBinaryMessage(cmd, msgId, data.getBytes(CharsetUtil.UTF_8));
    }

    public static ByteBuf makeBinaryMessage(short cmd, int msgId, byte[] byteData) {
        return PooledByteBufAllocator.DEFAULT.buffer(MessageBase.HEADER_LENGTH + byteData.length)
                .writeByte(cmd)
                .writeShort(msgId)
                .writeShort(byteData.length)
                .writeBytes(byteData);
    }

}
