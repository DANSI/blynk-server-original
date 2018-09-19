package cc.blynk.core.http;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cc.blynk.core.http.utils.ListUtils.subList;
import static cc.blynk.utils.http.MediaType.APPLICATION_JSON;
import static cc.blynk.utils.http.MediaType.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.MOVED_PERMANENTLY;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.12.15.
 */
public final class Response extends DefaultFullHttpResponse {

    private static final String JSON = APPLICATION_JSON + ";charset=utf-8";
    private static final String PLAIN_TEXT = TEXT_PLAIN + ";charset=utf-8";

    final static Response NO_RESPONSE = null;

    private Response(HttpVersion version, HttpResponseStatus status, String content, String contentType) {
        super(version, status, (
                content == null
                        ? Unpooled.EMPTY_BUFFER
                        : Unpooled.copiedBuffer(content, StandardCharsets.UTF_8))
        );
        fillHeaders(contentType);
    }

    private Response(HttpVersion version, HttpResponseStatus status, byte[] content, String contentType) {
        super(version, status, (content == null ? Unpooled.EMPTY_BUFFER : Unpooled.copiedBuffer(content)));
        fillHeaders(contentType);
    }

    private Response(HttpVersion version, HttpResponseStatus status) {
        super(version, status);
        headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                 .set(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                 .set(CONTENT_LENGTH, 0);
    }

    private void fillHeaders(String contentType) {
        headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                 .set(CONTENT_TYPE, contentType)
                 .set(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                 .set(CONTENT_LENGTH, content().readableBytes());
    }

    public static Response noResponse() {
        return NO_RESPONSE;
    }

    public static Response ok() {
        return new Response(HTTP_1_1, OK);
    }

    public static Response notFound() {
        return new Response(HTTP_1_1, NOT_FOUND);
    }

    public static Response forbidden() {
        return new Response(HTTP_1_1, FORBIDDEN);
    }

    public static Response forbidden(String error) {
        return new Response(HTTP_1_1, FORBIDDEN, error, PLAIN_TEXT);
    }

    public static Response badRequest() {
        return new Response(HTTP_1_1, BAD_REQUEST);
    }

    public static Response redirect(String url) {
        Response response = new Response(HTTP_1_1, MOVED_PERMANENTLY);
        response.headers()
                .set(LOCATION, url)
                .set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        return response;
    }

    public static Response badRequest(String message) {
        return new Response(HTTP_1_1, BAD_REQUEST, message, PLAIN_TEXT);
    }

    public static Response serverError() {
        return new Response(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }

    public static Response serverError(String message) {
        return new Response(HTTP_1_1, INTERNAL_SERVER_ERROR, message, PLAIN_TEXT);
    }

    public static Response ok(String data) {
        return new Response(HTTP_1_1, OK, data, JSON);
    }

    public static Response ok(String data, String contentType) {
        return new Response(HTTP_1_1, OK, data, contentType);
    }

    public static Response ok(byte[] data, String contentType) {
        return new Response(HTTP_1_1, OK, data, contentType);
    }


    public static Response ok(boolean bool) {
        return new Response(HTTP_1_1, OK, String.valueOf(bool), JSON);
    }

    public static Response ok(User user) {
        return ok(JsonParser.toJson(user));
    }

    public static Response ok(DashBoard dashBoard) {
        return ok(JsonParser.toJson(dashBoard));
    }

    public static Response ok(List<?> list, int page, int size) {
        String data = JsonParser.toJson(subList(list, page, size));
        return ok(data == null ? "[]" : data);
    }

    public static Response ok(Map<?, ?> map) {
        String data = JsonParser.toJson(map);
        return ok(data == null ? "{}" : data);
    }

    public static Response ok(Collection<?> list) {
        String data = JsonParser.toJson(list);
        return ok(data == null ? "[]" : data);
    }

    public static Response appendTotalCountHeader(Response response, int count) {
        response.headers().set("X-Total-Count", count);
        return response;
    }
}
