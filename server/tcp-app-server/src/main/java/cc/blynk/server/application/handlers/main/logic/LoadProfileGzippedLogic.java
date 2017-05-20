package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.db.model.FlashedToken;
import cc.blynk.utils.ParseUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.server.core.protocol.enums.Response.NO_DATA;
import static cc.blynk.utils.BlynkByteBufUtil.*;
import static cc.blynk.utils.JsonParser.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class LoadProfileGzippedLogic {

    private static final Logger log = LogManager.getLogger(LoadProfileGzippedLogic.class);

    private final UserDao userDao;
    private final DBManager dbManager;
    private final BlockingIOProcessor blockingIOProcessor;

    public LoadProfileGzippedLogic(Holder holder) {
        this.userDao = holder.userDao;
        this.dbManager = holder.dbManager;
        this.blockingIOProcessor = holder.blockingIOProcessor;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        //load all
        if (message.length == 0) {
            Profile profile = state.user.profile;
            write(ctx, gzipProfile(profile), message.id);
            return;
        }

        String[] parts = message.body.split(" |\0");
        if (parts.length == 1) {
            //load specific by id
            int dashId = ParseUtil.parseInt(message.body);
            DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);
            write(ctx, gzipDash(dash), message.id);
        } else {
            String token = parts[0];
            int dashId = ParseUtil.parseInt(parts[1]);
            String publishingEmail = parts[2];

            blockingIOProcessor.executeDB(() -> {
                try {
                    FlashedToken flashedToken = dbManager.selectFlashedToken(token);
                    if (flashedToken != null) {
                        User publishingUser = userDao.getByName(publishingEmail, state.userKey.appName);
                        DashBoard dash = publishingUser.profile.getDashByIdOrThrow(dashId);
                        write(ctx, gzipDashRestrictive(dash), message.id);
                    }
                } catch (Exception e) {
                    ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
                    log.error("Error getting publishing profile.", e.getMessage());
                }
            });
        }
    }

    public static void write(ChannelHandlerContext ctx, byte[] data, int msgId) {
        if (ctx.channel().isWritable()) {
            ByteBuf outputMsg;
            if (data == null) {
                outputMsg = makeResponse(msgId, NO_DATA);
            } else {
                outputMsg = makeBinaryMessage(LOAD_PROFILE_GZIPPED, msgId, data);
            }
            ctx.writeAndFlush(outputMsg, ctx.voidPromise());
        }
    }

}
