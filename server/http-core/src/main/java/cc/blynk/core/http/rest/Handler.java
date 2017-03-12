package cc.blynk.core.http.rest;

import cc.blynk.core.http.Response;
import cc.blynk.core.http.UriTemplate;
import cc.blynk.core.http.annotation.DELETE;
import cc.blynk.core.http.annotation.GET;
import cc.blynk.core.http.annotation.POST;
import cc.blynk.core.http.annotation.PUT;
import cc.blynk.core.http.rest.params.Param;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class Handler {

    private static final Logger log = LogManager.getLogger(Handler.class);

    public final UriTemplate uriTemplate;

    public HttpMethod httpMethod;

    public final Method classMethod;

    public final Object handler;

    public final Param[] params;

    public Handler(UriTemplate uriTemplate, Method method, Object handler, int paramsCount) {
        this.uriTemplate = uriTemplate;
        this.classMethod = method;
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

        this.params = new Param[paramsCount];
    }

    public Object[] fetchParams(URIDecoder uriDecoder) {
        Object[] res = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            res[i] = params[i].get(uriDecoder);
        }

        return res;
    }

    public FullHttpResponse invoke(Object[] params) {
        try {
            return (FullHttpResponse) classMethod.invoke(handler, params);
        } catch (Exception e) {
            log.error("Error invoking handler. Reason : {}.", e.getMessage());
            log.debug(e);
            return Response.serverError(e.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Handler)) return false;

        Handler that = (Handler) o;

        if (uriTemplate != null ? !uriTemplate.equals(that.uriTemplate) : that.uriTemplate != null) return false;
        return !(httpMethod != null ? !httpMethod.equals(that.httpMethod) : that.httpMethod != null);

    }

    @Override
    public int hashCode() {
        int result = uriTemplate != null ? uriTemplate.hashCode() : 0;
        result = 31 * result + (httpMethod != null ? httpMethod.hashCode() : 0);
        return result;
    }
}
