package cc.blynk.utils;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.01.18.
 */
public final class HttpUtil {

    private HttpUtil() {
    }

    /**
     * See HttpSignatureTest for more details
     */
    public static boolean isHttp(long httpHeader4Bytes) {
        return
                httpHeader4Bytes == 1195725856L || // 'GET '
                httpHeader4Bytes == 1347375956L || // 'POST'
                httpHeader4Bytes == 1347769376L || // 'PUT '
                httpHeader4Bytes == 1212498244L || // 'HEAD'
                httpHeader4Bytes == 1330664521L || // 'OPTI'
                httpHeader4Bytes == 1346458691L || // 'PATC'
                httpHeader4Bytes == 1145392197L || // 'DELE'
                httpHeader4Bytes == 1414676803L || // 'TRAC'
                httpHeader4Bytes == 1129270862L;   // 'CONN'
    }
}
