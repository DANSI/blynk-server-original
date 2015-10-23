package cc.blynk.server.core.application;

import cc.blynk.common.handlers.common.decoders.MessageDecoder;
import cc.blynk.common.handlers.common.encoders.MessageEncoder;
import cc.blynk.server.Holder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.handlers.app.AppChannelStateHandler;
import cc.blynk.server.handlers.app.auth.AppLoginHandler;
import cc.blynk.server.handlers.app.auth.RegisterHandler;
import cc.blynk.server.handlers.app.auth.sharing.AppShareLoginHandler;
import cc.blynk.server.handlers.common.UserNotLoggerHandler;
import cc.blynk.server.utils.SslUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

import javax.net.ssl.SSLEngine;

/**
 * Class responsible for handling all Application connections and netty pipeline initialization.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class AppServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public AppServer(Holder holder) {
        super(holder.props.getIntProperty("app.ssl.port"), holder.transportType);

        RegisterHandler registerHandler = new RegisterHandler(holder.userDao, holder.props.getProperty("allowed.users.list"));
        AppLoginHandler appLoginHandler = new AppLoginHandler(holder.props, holder.userDao, holder.sessionDao, holder.reportingDao, holder.blockingIOProcessor);
        AppChannelStateHandler appChannelStateHandler = new AppChannelStateHandler(holder.sessionDao);
        AppShareLoginHandler appShareLoginHandler = new AppShareLoginHandler(holder.props, holder.userDao, holder.sessionDao, holder.reportingDao);

        AppSslContext appSslContext = SslUtil.initSslContext(holder.props);

        int appTimeoutSecs = holder.props.getIntProperty("app.socket.idle.timeout", 0);
        log.debug("app.socket.idle.timeout = {}", appTimeoutSecs);

        this.channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                if (appTimeoutSecs > 0) {
                    pipeline.addLast(new ReadTimeoutHandler(appTimeoutSecs));
                }

                SSLEngine engine = appSslContext.sslContext.newEngine(ch.alloc());
                if (appSslContext.isMutualSSL) {
                    engine.setUseClientMode(false);
                    engine.setNeedClientAuth(true);
                }
                pipeline.addLast(new SslHandler(engine));

                //non-sharable handlers
                pipeline.addLast(appChannelStateHandler);
                pipeline.addLast(new MessageDecoder(holder.stats));
                pipeline.addLast(new MessageEncoder());

                //sharable business logic handlers initialized previously
                pipeline.addLast(registerHandler);
                pipeline.addLast(appLoginHandler);
                pipeline.addLast(appShareLoginHandler);
                pipeline.addLast(new UserNotLoggerHandler());
            }
        };

        log.info("Application server port {}.", port);
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "application";
    }

    @Override
    public void stop() {
        log.info("Shutting down SSL server...");
        super.stop();
    }

}
