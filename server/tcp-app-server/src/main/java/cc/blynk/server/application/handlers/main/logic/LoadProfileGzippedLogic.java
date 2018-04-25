package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.model.serialization.JsonParser.gzipDash;
import static cc.blynk.server.core.model.serialization.JsonParser.gzipDashRestrictive;
import static cc.blynk.server.core.model.serialization.JsonParser.gzipProfile;
import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommand;
import static cc.blynk.server.internal.CommonByteBufUtil.makeBinaryMessage;
import static cc.blynk.server.internal.CommonByteBufUtil.noData;

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
        var msgId = message.id;

        if (message.body.length() == 0) {
            Profile profile = state.user.profile;
            write(ctx, gzipProfile(profile), msgId);
            return;
        }

        var parts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);
        if (parts.length == 1) {
            //load specific by id
            var dashId = Integer.parseInt(message.body);
            var dash = state.user.profile.getDashByIdOrThrow(dashId);
            write(ctx, gzipDash(dash), msgId);
        } else {
            var token = parts[0];
            var dashId = Integer.parseInt(parts[1]);
            var publishingEmail = parts[2];
            //this is for simplification of testing.
            var appName = parts.length == 4 ? parts[3] : state.userKey.appName;

            blockingIOProcessor.executeDB(() -> {
                try {
                    var flashedToken = dbManager.selectFlashedToken(token);
                    if (flashedToken != null) {
                        var publishingUser = userDao.getByName(publishingEmail, appName);
                        var dash = publishingUser.profile.getDashByIdOrThrow(dashId);
                        //todo ugly. but ok for now
                        var copyString = JsonParser.toJsonRestrictiveDashboard(dash);
                        var copyDash = JsonParser.parseDashboard(copyString, msgId);
                        copyDash.eraseValues();
                        write(ctx, gzipDashRestrictive(copyDash), msgId);
                    }
                } catch (Exception e) {
                    ctx.writeAndFlush(illegalCommand(msgId), ctx.voidPromise());
                    log.error("Error getting publishing profile.", e.getMessage());
                }
            });
        }
    }

    public static void write(ChannelHandlerContext ctx, byte[] data, int msgId) {
        if (ctx.channel().isWritable()) {
            var outputMsg = makeResponse(data, msgId);
            ctx.writeAndFlush(outputMsg, ctx.voidPromise());
        }
    }

    private static MessageBase makeResponse(byte[] data, int msgId) {
        if (data == null) {
            return noData(msgId);
        }
        return makeBinaryMessage(LOAD_PROFILE_GZIPPED, msgId, data);
    }

}
