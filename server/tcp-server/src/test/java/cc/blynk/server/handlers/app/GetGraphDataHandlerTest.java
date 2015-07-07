package cc.blynk.server.handlers.app;

import cc.blynk.server.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.InflaterInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.07.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class GetGraphDataHandlerTest extends TestBase {

    private static String decompress(byte[] bytes) throws IOException {
        InputStream in = new InflaterInputStream(new ByteArrayInputStream(bytes));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[4096];
            int len;
            while((len = in.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return new String(baos.toByteArray());
        } catch (IOException e) {
            throw new AssertionError(e);
        }

    }

    @Test
    public void testCompressAndDecompress() throws IOException {
        Queue<String> queue = new LinkedList<>();

        int dataLength = 0;
        for (int i = 0; i < 1000; i++) {
            long ts = System.currentTimeMillis();
            String body = ("aw 1 x " + ts).replace("x", String.valueOf(i)).replace(" ", "\0");
            queue.offer(body);
            dataLength += body.length();
        }

        System.out.println("Size before compression : " + dataLength);
        byte[] compressedData = GetGraphDataHandler.compress(queue, 1);
        System.out.println("Size after compression : " + compressedData.length + ". Compress rate " + ((double) dataLength / compressedData.length));
        assertNotNull(compressedData);
        String result = decompress(compressedData);
        String[] splitted = result.split("\0");
        assertEquals(4000, splitted.length);

        for (int i = 0; i < 1000; i++) {
            assertEquals("aw", splitted[i * 4]);
            assertEquals("1", splitted[i * 4 + 1]);
            assertEquals(String.valueOf(i), splitted[i * 4 + 2]);
        }

        //System.out.println(result);
    }

}
