package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.sharing.auth.AppShareStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.ParseUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.model.serialization.JsonParser.gzipDashRestrictive;
import static cc.blynk.server.core.model.serialization.JsonParser.gzipProfileRestrictive;
import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.server.internal.BlynkByteBufUtil.makeBinaryMessage;
import static cc.blynk.server.internal.BlynkByteBufUtil.serverError;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class LoadSharedProfileGzippedLogic {

    private static final Logger log = LogManager.getLogger(LoadSharedProfileGzippedLogic.class);

    private LoadSharedProfileGzippedLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, AppShareStateHolder state, StringMessage message) {
        byte[] data;
        if (message.length == 0) {
            DashBoard dash = state.user.profile.getDashByIdOrThrow(state.dashId);
            Profile profile = new Profile();
            profile.dashBoards = new DashBoard[] {dash};
            data = gzipProfileRestrictive(profile);
        } else {
            //load specific by id
            int dashId = ParseUtil.parseInt(message.body);
            DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);
            data = gzipDashRestrictive(dash);
        }
        write(ctx, data, message.id);
    }

    private static void write(ChannelHandlerContext ctx, byte[] data, int msgId) {
        if (ctx.channel().isWritable()) {
            ByteBuf outputMsg;
            if (data.length > 65_535) {
                log.error("User profile is too big. Size : {}", data.length);
                outputMsg = serverError(msgId);
            } else {
                outputMsg = makeBinaryMessage(LOAD_PROFILE_GZIPPED, msgId, data);
            }
            ctx.writeAndFlush(outputMsg, ctx.voidPromise());
        }
    }

}
