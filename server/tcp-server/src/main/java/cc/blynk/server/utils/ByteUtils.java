package cc.blynk.server.utils;

import cc.blynk.server.exceptions.GetGraphDataException;
import cc.blynk.server.exceptions.NoDataException;
import io.netty.util.CharsetUtil;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.09.15.
 */
public class ByteUtils {

    public static final int REPORTING_RECORD_SIZE_BYTES = 16;

    public static byte[] compress(String value, int msgId) {
        byte[] stringData = value.getBytes(CharsetUtil.UTF_8);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(stringData.length);

        try (OutputStream out = new DeflaterOutputStream(baos)) {
            out.write(stringData);
        } catch (Exception ioe) {
            //todo refactor exception
            throw new NoDataException(msgId);
        }
        return baos.toByteArray();
    }

    public static byte[] compress(byte[][] values, int msgId) {
        //todo calculate size
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);

        try (OutputStream out = new DeflaterOutputStream(baos)) {
            for (byte[] data : values) {
                ByteBuffer bb = ByteBuffer.allocate(4);
                bb.putInt(data.length / REPORTING_RECORD_SIZE_BYTES);
                out.write(bb.array());
                out.write(data);
            }
        } catch (Exception ioe) {
            //todo refactor exception
            throw new GetGraphDataException(msgId);
        }
        return baos.toByteArray();
    }

    public static byte[] compress(int dashId, byte[][] values, int msgId) {
        //todo calculate size
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);

        try (OutputStream out = new DeflaterOutputStream(baos)) {
            out.write(ByteBuffer.allocate(4).putInt(dashId).array());
            for (byte[] data : values) {
                ByteBuffer bb = ByteBuffer.allocate(4);
                bb.putInt(data.length / REPORTING_RECORD_SIZE_BYTES);
                out.write(bb.array());
                out.write(data);
            }
        } catch (Exception ioe) {
            //todo refactor exception
            throw new GetGraphDataException(msgId);
        }
        return baos.toByteArray();
    }


}
