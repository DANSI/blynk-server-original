package cc.blynk.server.handlers.app;

import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.TestBase;
import cc.blynk.server.model.graph.GraphKey;
import cc.blynk.server.utils.ByteUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.InflaterInputStream;

import static cc.blynk.server.utils.ByteUtils.*;
import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.07.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class GetGraphDataHandlerTest extends TestBase {

    private static byte[] decompress(byte[] bytes) {
        InputStream in = new InflaterInputStream(new ByteArrayInputStream(bytes));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[4096];
            int len;
            while((len = in.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }

    }

    private static byte[] toByteArray(GraphKey storeMessage) {
        ByteBuffer bb = ByteBuffer.allocate(REPORTING_RECORD_SIZE_BYTES);
        bb.putDouble(Double.valueOf(storeMessage.value));
        bb.putLong(storeMessage.ts);
        return bb.array();
    }

    @Test
    public void testCompressAndDecompress() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(1000 * REPORTING_RECORD_SIZE_BYTES);

        int dataLength = 0;
        for (int i = 0; i < 1000; i++) {
            long ts = System.currentTimeMillis();
            GraphKey mes = new GraphKey(1, ("aw 1 " + i).split(StringUtils.BODY_SEPARATOR_STRING), ts);
            bb.put(toByteArray(mes));
            dataLength += REPORTING_RECORD_SIZE_BYTES;
        }

        System.out.println("Size before compression : " + dataLength);
        byte[][] data = new byte[1][];
        data[0] = bb.array();

        byte[] compressedData = ByteUtils.compress(data, 1);
        System.out.println("Size after compression : " + compressedData.length + ". Compress rate " + ((double) dataLength / compressedData.length));
        assertNotNull(compressedData);
        ByteBuffer result = ByteBuffer.wrap(decompress(compressedData));

        assertEquals(1000 * REPORTING_RECORD_SIZE_BYTES + 4, result.capacity());

        int size = result.getInt();
        assertEquals(1000, size);

        for (int i = 0; i < 1000; i++) {
            assertEquals((double) i, result.getDouble(), 0.001);
            result.getLong();
        }

        //System.out.println(result);
    }

}
