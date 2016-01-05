package cc.blynk.server.core.application;

import io.netty.handler.ssl.SslContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.09.15.
 */
public class AppSslContext {

    boolean isMutualSSL;

    SslContext sslContext;

    public AppSslContext(boolean isMutualSSL, SslContext sslContext) {
        this.isMutualSSL = isMutualSSL;
        this.sslContext = sslContext;
    }
}
