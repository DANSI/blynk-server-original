package cc.blynk.server.api.http.logic;

import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.01.18.
 */
public class HttpSignatureTest {

    @Test
    public void printSignatures() {
        System.out.println(getLongVal("GET ")); //1195725856
        System.out.println(getLongVal("POST")); //1347375956
        System.out.println(getLongVal("PUT ")); //1347769376
        System.out.println(getLongVal("HEAD")); //1212498244
        System.out.println(getLongVal("OPTI")); //1330664521
        System.out.println(getLongVal("PATC")); //1346458691
        System.out.println(getLongVal("DELE")); //1145392197
        System.out.println(getLongVal("TRAC")); //1414676803
        System.out.println(getLongVal("CONN")); //1129270862
    }

    private static long getLongVal(String requestStart) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        for (char c : requestStart.toCharArray()) {
            bb.put((byte) c);
        }
        bb.flip();
        return Integer.toUnsignedLong(bb.getInt());
    }

}
