package cc.blynk.server.application.handlers.main.auth;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.protocol.model.messages.appllication.GetServerMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.utils.StringUtils;
import cc.blynk.utils.validators.BlynkEmailValidator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static cc.blynk.utils.BlynkByteBufUtil.*;


/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.10.16.
 */
@ChannelHandler.Sharable
public class GetServerHandler extends SimpleChannelInboundHandler<GetServerMessage> {

    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;
    private final UserDao userDao;
    private final String currentIp;

    public GetServerHandler(Holder holder, String[] ips) {
        super();
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.dbManager = holder.dbManager;
        this.userDao = holder.userDao;
        this.currentIp = holder.host;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GetServerMessage msg) throws Exception {
        final String[] parts = StringUtils.split2(msg.body);

        if (parts.length < 2) {
            ctx.writeAndFlush(illegalCommand(msg.id), ctx.voidPromise());
            return;
        }

        final String email = parts[0];
        final String appName = parts[1];

        if (appName == null || appName.isEmpty() || appName.length() > 100) {
            ctx.writeAndFlush(illegalCommand(msg.id), ctx.voidPromise());
            return;
        }

        if (BlynkEmailValidator.isNotValidEmail(email)) {
            ctx.writeAndFlush(illegalCommandBody(msg.id), ctx.voidPromise());
            return;
        }

        if (userDao.contains(email, appName)) {
            //user exists on current server. so returning ip of current server
            ctx.writeAndFlush(makeASCIIStringMessage(msg.command, msg.id, currentIp), ctx.voidPromise());
        } else {
            //user is on other server
            blockingIOProcessor.executeDB(() -> {
                String userServer = dbManager.getServerByUser(email);
                if (userServer == null) {
                    //user not registered yet anywhere
                    dbManager.assignServerToUser(email, currentIp);
                    userServer = currentIp;
                }

                ctx.writeAndFlush(makeASCIIStringMessage(msg.command, msg.id, userServer), ctx.voidPromise());
            });
        }
    }

}
