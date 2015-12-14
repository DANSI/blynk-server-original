package cc.blynk.server.handlers.http.rest;

import io.netty.handler.codec.http.HttpMethod;
import org.glassfish.jersey.uri.UriTemplate;

import javax.ws.rs.*;
import java.lang.reflect.Method;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class HandlerHolder {

    public UriTemplate uriTemplate;

    public HttpMethod httpMethod;

    public Method method;

    public Object handler;

    public MethodParam[] params;

    public HandlerHolder(UriTemplate uriTemplate, Method method, Object handler, int paramsCount) {
        this.uriTemplate = uriTemplate;
        this.method = method;
        this.handler = handler;

        if (method.isAnnotationPresent(GET.class)) {
            this.httpMethod = HttpMethod.GET;
        }
        if (method.isAnnotationPresent(POST.class)) {
            this.httpMethod = HttpMethod.POST;
        }
        if (method.isAnnotationPresent(PUT.class)) {
            this.httpMethod = HttpMethod.PUT;
        }
        if (method.isAnnotationPresent(DELETE.class)) {
            this.httpMethod = HttpMethod.DELETE;
        }
        if (method.isAnnotationPresent(HEAD.class)) {
            this.httpMethod = HttpMethod.HEAD;
        }
        if (method.isAnnotationPresent(OPTIONS.class)) {
            this.httpMethod = HttpMethod.OPTIONS;
        }

        this.params = new MethodParam[paramsCount];
    }

    public Object[] fetchParams(URIDecoder uriDecoder) {
        Object[] res = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            res[i] = params[i].get(uriDecoder);
        }

        return res;
    }

}
