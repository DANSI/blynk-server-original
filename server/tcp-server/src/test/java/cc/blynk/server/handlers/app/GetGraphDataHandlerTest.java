package cc.blynk.server.handlers.app;

import cc.blynk.server.TestBase;
import cc.blynk.server.dao.graph.GraphKey;
import cc.blynk.server.dao.graph.StoreMessage;
import cc.blynk.server.handlers.app.logic.GetGraphDataLogic;
import cc.blynk.server.model.enums.PinType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
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

    private static String decompress(byte[] bytes) {
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
        Queue<StoreMessage> queue = new LinkedList<>();

        int dataLength = 0;
        for (int i = 0; i < 1000; i++) {
            long ts = System.currentTimeMillis();
            StoreMessage mes = new StoreMessage(new GraphKey(1, (byte) 1, PinType.ANALOG), String.valueOf(i), ts);
            queue.offer(mes);
            dataLength += mes.toString().length();
        }

        System.out.println("Size before compression : " + dataLength);
        byte[] compressedData = GetGraphDataLogic.compress(new Collection[] {queue}, 1);
        System.out.println("Size after compression : " + compressedData.length + ". Compress rate " + ((double) dataLength / compressedData.length));
        assertNotNull(compressedData);
        String result = decompress(compressedData);
        String[] splitted = result.split(" ");
        assertEquals(2000, splitted.length);

        for (int i = 0; i < 1000; i++) {
            assertEquals(String.valueOf(i), splitted[i * 2]);
        }

        //System.out.println(result);
    }

}
