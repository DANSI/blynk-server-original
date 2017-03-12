package cc.blynk.core.http;

import static io.netty.handler.codec.http.HttpResponseStatus.MOVED_PERMANENTLY;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.12.15.
 */
public class ForwardResponse extends Response {

    public final String url;

    public ForwardResponse(String url) {
        super(HTTP_1_1, MOVED_PERMANENTLY);
        this.url = url;
    }
}
