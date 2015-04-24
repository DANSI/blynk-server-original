package cc.blynk.server.handlers.app;

import cc.blynk.common.model.messages.protocol.appllication.SaveProfileMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.model.Profile;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.utils.JsonParser;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class SaveProfileHandler extends BaseSimpleChannelInboundHandler<SaveProfileMessage> {

    //I have to use volatile for reloadable props to be sure updated value will be visible by all threads
    private volatile int DASH_MAX_LIMIT;

    //I have to use volatile for reloadable props to be sure updated value will be visible by all threads
    private volatile int USER_PROFILE_MAX_SIZE;

    public SaveProfileHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        super(props, userRegistry, sessionsHolder);
        updateProperties(props);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, User user, SaveProfileMessage message) {
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

        if (profile.getDashBoards() != null && profile.getDashBoards().length > DASH_MAX_LIMIT) {
            throw new NotAllowedException(
                    String.format("Not allowed to create more than %s dashboards.", DASH_MAX_LIMIT), message.id);
        }

        log.info("Trying save user profile.");

        profile.calcGraphPins();

        user.setProfile(profile);
        ctx.writeAndFlush(produce(message.id, OK));
    }

    @Override
    public void updateProperties(ServerProperties props) {
        super.updateProperties(props);
        try {
            this.DASH_MAX_LIMIT = props.getIntProperty("user.dashboard.max.limit");
        } catch (RuntimeException e) {
            //error already logged, so do nothing.
        }
        try {
            this.USER_PROFILE_MAX_SIZE = props.getIntProperty("user.profile.max.size") * 1024;
        } catch (RuntimeException e) {
            //error already logged, so do nothing.
        }
    }

}
