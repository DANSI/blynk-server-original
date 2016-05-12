package cc.blynk.server.core.protocol.handlers;

import cc.blynk.server.core.protocol.exceptions.BaseServerException;
import cc.blynk.server.core.protocol.exceptions.UnsupportedCommandException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.handler.timeout.ReadTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLException;
import java.io.IOException;

import static cc.blynk.utils.ByteBufUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/11/2015.
 */
public interface DefaultExceptionHandler {

    Logger log = LogManager.getLogger(DefaultExceptionHandler.class);

    default void handleGeneralException(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof BaseServerException) {
            BaseServerException baseServerException = (BaseServerException) cause;
            //no need for stack trace for known exceptions
            log.error(baseServerException.getMessage());
            ctx.writeAndFlush(makeResponse(baseServerException.msgId, baseServerException.errorCode), ctx.voidPromise());
        } else {
            handleUnexpectedException(ctx, cause);
        }
    }

    default void handleUnexpectedException(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ReadTimeoutException) {
            log.trace("Channel was inactive for a long period. Closing...");
            //channel is already closed here by ReadTimeoutHandler
        } else if (cause instanceof DecoderException) {
            if (cause.getCause() instanceof UnsupportedCommandException) {
                log.error("Input command is invalid. Closing socket. Reason {}. Address {}", cause.getMessage(), ctx.channel().remoteAddress());
            } else if (cause.getCause() instanceof SSLException) {
                log.error("Unsecured connection attempt. Channel : {}. Reason : {}", ctx.channel().remoteAddress(), cause.getMessage());
            } else {
                log.error("DecoderException.", cause);
            }
            ctx.close();
        } else if (cause instanceof NotSslRecordException) {
            log.error("Not secure connection attempt detected. {}. IP {}", cause.getMessage(), ctx.channel().remoteAddress());
            ctx.close();
        } else if (cause instanceof SSLException) {
            log.error("SSL exception. {}.", cause.getMessage());
            ctx.close();
        } else if (cause instanceof IOException) {
            log.debug("Blynk server IOException.", cause);
        } else {
            log.error("Unexpected error!!!", cause);
            log.error("Handler class : {}. Name : {}", ctx.handler().getClass(), ctx.name());
        }

    }

}
