package cc.blynk.utils;

import cc.blynk.core.http.UriTemplate;
import cc.blynk.core.http.annotation.Consumes;
import cc.blynk.core.http.annotation.Context;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.core.http.rest.HandlerWrapper;
import cc.blynk.core.http.rest.params.BodyParam;
import cc.blynk.core.http.rest.params.ContextParam;
import cc.blynk.core.http.rest.params.FormParam;
import cc.blynk.core.http.rest.params.PathParam;
import cc.blynk.core.http.rest.params.QueryParam;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.utils.http.MediaType;
import io.netty.channel.ChannelHandlerContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public final class AnnotationsUtil {

    private AnnotationsUtil() {
    }

    public static HandlerWrapper[] register(String rootPath, Object o, GlobalStats globalStats) {
        return registerHandler(rootPath, o, globalStats);
    }

    private static HandlerWrapper[] registerHandler(String rootPath, Object handler, GlobalStats globalStats) {
        Class<?> handlerClass = handler.getClass();
        Annotation pathAnnotation = handlerClass.getAnnotation(Path.class);
        String handlerMainPath = ((Path) pathAnnotation).value();

        List<HandlerWrapper> processors = new ArrayList<>();

        for (Method method : handlerClass.getMethods()) {
            Annotation consumes = method.getAnnotation(Consumes.class);
            String contentType = MediaType.APPLICATION_JSON;
            if (consumes != null) {
                contentType = ((Consumes) consumes).value()[0];
            }

            Annotation path = method.getAnnotation(Path.class);
            if (path != null) {
                String fullPath = rootPath + handlerMainPath + ((Path) path).value();
                UriTemplate uriTemplate = new UriTemplate(fullPath);

                HandlerWrapper handlerHolder = new HandlerWrapper(uriTemplate, method, handler, globalStats);

                for (int i = 0; i < method.getParameterCount(); i++) {
                    Parameter parameter = method.getParameters()[i];

                    cc.blynk.core.http.annotation.QueryParam queryParamAnnotation =
                            parameter.getAnnotation(cc.blynk.core.http.annotation.QueryParam.class);
                    if (queryParamAnnotation != null) {
                        handlerHolder.params[i] = new QueryParam(queryParamAnnotation.value(), parameter.getType());
                    }

                    cc.blynk.core.http.annotation.PathParam pathParamAnnotation =
                            parameter.getAnnotation(cc.blynk.core.http.annotation.PathParam.class);
                    if (pathParamAnnotation != null) {
                        handlerHolder.params[i] = new PathParam(pathParamAnnotation.value(), parameter.getType());
                    }

                    cc.blynk.core.http.annotation.FormParam formParamAnnotation =
                            parameter.getAnnotation(cc.blynk.core.http.annotation.FormParam.class);
                    if (formParamAnnotation != null) {
                        handlerHolder.params[i] = new FormParam(formParamAnnotation.value(), parameter.getType());
                    }

                    Annotation contextAnnotation = parameter.getAnnotation(Context.class);
                    if (contextAnnotation != null) {
                        handlerHolder.params[i] = new ContextParam(ChannelHandlerContext.class);
                    }

                    if (pathParamAnnotation == null && queryParamAnnotation == null && formParamAnnotation == null
                            && contextAnnotation == null) {
                        handlerHolder.params[i] =
                                new BodyParam(parameter.getName(), parameter.getType(), contentType);
                    }
                }

                processors.add(handlerHolder);
            }
        }

        return processors.toArray(new HandlerWrapper[processors.size()]);
    }

}
