package cc.blynk.utils;

import cc.blynk.server.core.protocol.exceptions.GetGraphDataException;
import io.netty.util.CharsetUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.09.15.
 */
public class ByteUtils {

    public static final int REPORTING_RECORD_SIZE_BYTES = 16;

    public static byte[] compress(String value) throws IOException {
        byte[] stringData = value.getBytes(CharsetUtil.UTF_8);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(stringData.length);

        try (OutputStream out = new DeflaterOutputStream(baos)) {
            out.write(stringData);
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
        } catch (IOException ioe) {
            //todo refactor exception
            throw new GetGraphDataException(msgId);
        }
        return baos.toByteArray();
    }

    public static byte[] compress(int dashId, byte[][] values) throws IOException {
        //todo calculate size
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);

        try (OutputStream out = new DeflaterOutputStream(baos)) {
            writeInt(out, dashId);
            for (byte[] data : values) {
                writeInt(out, data.length / REPORTING_RECORD_SIZE_BYTES);
                out.write(data);
            }
        }
        return baos.toByteArray();
    }

    private static void writeInt(OutputStream out, int value) throws IOException {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>>  8) & 0xFF);
        out.write((value) & 0xFF);
    }

    //for tests only
    public static byte[] decompress(byte[] bytes) throws IOException {
        try (InputStream in = new InflaterInputStream(new ByteArrayInputStream(bytes))) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } catch (IOException e) {

            throw new AssertionError(e);
        }
    }
}
