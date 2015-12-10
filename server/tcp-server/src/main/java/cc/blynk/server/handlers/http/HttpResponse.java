package cc.blynk.server.handlers.http;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.12.15.
 */
public class HttpResponse extends DefaultFullHttpResponse {

    public HttpResponse(HttpVersion version, HttpResponseStatus status, String content, String contentType) {
        super(version, status, (content == null ? Unpooled.EMPTY_BUFFER : Unpooled.copiedBuffer(content, StandardCharsets.UTF_8)));
        headers().set(CONTENT_TYPE, contentType);
        headers().set(CONTENT_LENGTH, content().readableBytes());
        headers().set("Access-Control-Allow-Origin", "*");
    }

    public HttpResponse(HttpVersion version, HttpResponseStatus status) {
        super(version, status);
    }
}
