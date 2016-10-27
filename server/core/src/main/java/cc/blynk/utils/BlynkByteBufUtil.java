package cc.blynk.utils;

import cc.blynk.server.core.protocol.enums.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;

import static cc.blynk.server.core.protocol.enums.Response.OK;
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

    public static ByteBuf ok(int msgId) {
        return makeResponse(msgId, OK);
    }

    public static ByteBuf makeResponse(int msgId, int responseCode) {
        return PooledByteBufAllocator.DEFAULT.buffer(HEADER_LENGTH)
                .writeByte(Command.RESPONSE)
                .writeShort(msgId)
                .writeShort(responseCode);
    }

    public static ByteBuf makeStringMessage(short cmd, int msgId, String data) {
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(HEADER_LENGTH + ByteBufUtil.utf8MaxBytes(data));
        byteBuf.writerIndex(HEADER_LENGTH);
        ByteBufUtil.writeUtf8(byteBuf, data);

        byteBuf.setByte(0, cmd)
               .setShort(1, msgId)
               .setShort(3, byteBuf.writerIndex() - HEADER_LENGTH);

        return byteBuf;
    }

    public static ByteBuf makeBinaryMessage(short cmd, int msgId, byte[] byteData) {
        return PooledByteBufAllocator.DEFAULT.buffer(HEADER_LENGTH + byteData.length)
                .writeByte(cmd)
                .writeShort(msgId)
                .writeShort(byteData.length)
                .writeBytes(byteData);
    }

}
