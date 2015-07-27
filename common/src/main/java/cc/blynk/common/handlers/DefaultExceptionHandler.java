package cc.blynk.common.handlers;

import cc.blynk.common.exceptions.BaseServerException;
import cc.blynk.common.exceptions.UnsupportedCommandException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.handler.timeout.ReadTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import javax.net.ssl.SSLException;
import java.io.IOException;

import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/11/2015.
 */
public interface DefaultExceptionHandler {

    Logger log = LogManager.getLogger(DefaultExceptionHandler.class);

    default void handleGeneralException(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof BaseServerException) {
            handleAppException(ctx, (BaseServerException) cause);
        } else {
            handleUnexpectedException(ctx, cause);
        }
    }

    default void handleUnexpectedException(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ReadTimeoutException) {
            log.trace("Channel was inactive for a long period. Closing...");
            //channel is already closed here by ReadTimeoutHandler
        } else if (cause instanceof DecoderException && cause.getCause() instanceof UnsupportedCommandException) {
            log.error("Input command is invalid. Closing socket. Reason {}. Address {}", cause.getMessage(), ctx.channel().remoteAddress());
            ctx.close();
        } else if (cause instanceof DecoderException && cause.getCause() instanceof SSLException) {
            log.error("WARNING. Unsecured connection attempt. Channel : {}. Reason : {}", ctx.channel().remoteAddress(), cause.getMessage());
            ctx.close();
        } else if (cause instanceof NotSslRecordException) {
            log.error("Not secure connection attempt detected. {}. IP {}", cause.getMessage(), ctx.channel().remoteAddress());
            ctx.close();
        } else if (cause instanceof SSLException) {
            log.error("SSL exception. {}.", cause.getMessage());
            ctx.close();
        } else if (cause instanceof IOException) {
            String errorMessage = cause.getMessage() == null ? "" : cause.getMessage();
            //all this are expected when user goes offline without closing socket correctly...
            switch (errorMessage) {
                case "Connection reset by peer" :
                case "No route to host" :
                case "Connection timed out" :
                case "syscall:read(...)() failed: Connection reset by peer" : //epoll connection time out, should be fixed on netty
                    log.debug("Client goes offline. Reason : {}", cause.getMessage());
                    break;
                default:
                    log.error("Blynk server IOException. {}", cause.getMessage());
                    break;
            }

        } else {
            log.error("Unexpected error!!!", cause);
            log.error("Handler class : {}. Name : {}", ctx.handler().getClass(), ctx.name());
        }

    }

    default void handleAppException(ChannelHandlerContext ctx, BaseServerException baseServerException) {
        //no need for stack trace for known exceptions
        log.error(baseServerException.getMessage());
        try {
            //todo handle exception here?
            ctx.writeAndFlush(produce(baseServerException));
        } finally {
            //cleanup logging context in case error happened.
            ThreadContext.clearMap();
        }
    }

}
