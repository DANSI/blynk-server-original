package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.sharing.auth.MobileShareStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.model.serialization.JsonParser.gzipDashRestrictive;
import static cc.blynk.server.core.model.serialization.JsonParser.gzipProfileRestrictive;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class LoadSharedProfileGzippedLogic {

    private LoadSharedProfileGzippedLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, MobileShareStateHolder state, StringMessage message) {
        byte[] data;
        var user = state.user;
        if (message.body.length() == 0) {
            var dash = user.profile.getDashByIdOrThrow(state.dashId);
            var profile = new Profile();
            profile.dashBoards = new DashBoard[] {dash};
            data = gzipProfileRestrictive(profile);
        } else {
            //load specific by id
            var dashId = Integer.parseInt(message.body);
            var dash = user.profile.getDashByIdOrThrow(dashId);
            data = gzipDashRestrictive(dash);
        }
        MobileLoadProfileGzippedLogic.write(ctx, data, message.id);
    }

}
