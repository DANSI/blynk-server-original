package cc.blynk.server.handlers.app.main.logic.dashboard;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.notifications.Notification;
import cc.blynk.server.utils.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class SaveDashLogic {

    private static final Logger log = LogManager.getLogger(SaveDashLogic.class);

    private final int DASH_MAX_SIZE;

    public SaveDashLogic(ServerProperties props) {
        this.DASH_MAX_SIZE = props.getIntProperty("user.profile.max.size") * 1024;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashString = message.body;

        //expecting message with 2 parts
        if (dashString == null || dashString.equals("")) {
            throw new IllegalCommandException("Income create dash message is empty.", message.id);
        }

        if (dashString.length() > DASH_MAX_SIZE) {
            throw new NotAllowedException("User dashboard is larger then limit.", message.id);
        }

        log.debug("Trying to parse user dash : {}", dashString);
        DashBoard updatedDash = JsonParser.parseDashboard(dashString, message.id);

        log.info("Saving dashboard.");

        int index = user.profile.getDashIndex(updatedDash.id, message.id);
        //do not accept isActive field from "saveDash" command
        updatedDash.isActive = user.profile.dashBoards[index].isActive;

        Notification newNotification = updatedDash.getWidgetByType(Notification.class);
        if (newNotification != null) {
            Notification oldNotification = user.profile.dashBoards[index].getWidgetByType(Notification.class);
            if (oldNotification != null) {
                newNotification.iOSTokens = oldNotification.iOSTokens;
                newNotification.androidTokens = oldNotification.androidTokens;
            }
        }

        user.profile.dashBoards[index] = updatedDash;
        user.lastModifiedTs = System.currentTimeMillis();

        ctx.writeAndFlush(produce(message.id, OK));
    }

}
