package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.sharing.auth.AppShareStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.model.serialization.JsonParser.gzipDashRestrictive;
import static cc.blynk.server.core.model.serialization.JsonParser.gzipProfileRestrictive;

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
        User user = state.user;
        if (message.body.length() == 0) {
            DashBoard dash = user.profile.getDashByIdOrThrow(state.dashId);
            Profile profile = new Profile();
            profile.dashBoards = new DashBoard[] {dash};
            data = gzipProfileRestrictive(profile);
        } else {
            //load specific by id
            int dashId = Integer.parseInt(message.body);
            DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
            data = gzipDashRestrictive(dash);
        }
        LoadProfileGzippedLogic.write(ctx, data, message.id, user.email, state.isNewProtocol());
    }

}
