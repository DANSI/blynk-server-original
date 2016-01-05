package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@Deprecated
public class SaveProfileLogic {

    private static final Logger log = LogManager.getLogger(SaveProfileLogic.class);

    private final int DASH_MAX_LIMIT;
    private final int USER_PROFILE_MAX_SIZE;

    public SaveProfileLogic(int dashMaxLimit, int dashMaxSize) {
        this.DASH_MAX_LIMIT = dashMaxLimit;
        this.USER_PROFILE_MAX_SIZE = dashMaxSize;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String userProfileString = message.body;

        //expecting message with 2 parts
        if (userProfileString == null || userProfileString.equals("")) {
            throw new IllegalCommandException("Save Profile Handler. Income profile message is empty.", message.id);
        }

        if (userProfileString.length() > USER_PROFILE_MAX_SIZE) {
            throw new NotAllowedException(String.format("User profile size is larger than %d bytes.", USER_PROFILE_MAX_SIZE), message.id);
        }

        log.debug("Trying to parse user profile : {}", userProfileString);
        Profile profile = JsonParser.parseProfile(userProfileString, message.id);

        if (profile.dashBoards.length > DASH_MAX_LIMIT) {
            throw new NotAllowedException(
                    String.format("Not allowed to create more than %s dashboards.", DASH_MAX_LIMIT), message.id);
        }

        log.info("Trying save user profile.");

        user.setProfile(profile);
        ctx.writeAndFlush(new ResponseMessage(message.id, OK));
    }

}
