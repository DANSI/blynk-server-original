package cc.blynk.server.application.handlers.main.auth;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.ProvisionType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.appllication.RegisterMessage;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.StringUtils;
import cc.blynk.utils.TokenGeneratorUtil;
import cc.blynk.utils.validators.BlynkEmailValidator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static cc.blynk.server.internal.CommonByteBufUtil.alreadyRegistered;
import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommand;
import static cc.blynk.server.internal.CommonByteBufUtil.notAllowed;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * Process register message.
 * Divides input string by nil char on 3 parts:
 * "username" "password" "appName".
 * Checks if user not registered yet. If not - registering.
 *
 * For instance, incoming register message may be : "user@mail.ua my_password"
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
@ChannelHandler.Sharable
public class RegisterHandler extends SimpleChannelInboundHandler<RegisterMessage> {

    private static final Logger log = LogManager.getLogger(RegisterHandler.class);

    private final UserDao userDao;
    private final TokenManager tokenManager;
    private final TimerWorker timerWorker;
    private final MailWrapper mailWrapper;
    private final BlockingIOProcessor blockingIOProcessor;
    private final Set<String> allowedUsers;

    public RegisterHandler(Holder holder) {
        this(holder.userDao, holder.tokenManager, holder.timerWorker,
                holder.mailWrapper, holder.blockingIOProcessor,
                holder.props.getCommaSeparatedValueAsArray("allowed.users.list"));
    }

    //for tests only
    RegisterHandler(UserDao userDao, TokenManager tokenManager, TimerWorker timerWorker,
                    MailWrapper mailWrapper, BlockingIOProcessor blockingIOProcessor, String[] allowedUsersArray) {
        this.userDao = userDao;
        this.tokenManager = tokenManager;
        this.timerWorker = timerWorker;
        this.mailWrapper = mailWrapper;
        this.blockingIOProcessor = blockingIOProcessor;
        if (allowedUsersArray != null && allowedUsersArray.length > 0
                && allowedUsersArray[0] != null && !allowedUsersArray[0].isEmpty()) {
            allowedUsers = new HashSet<>(Arrays.asList(allowedUsersArray));
            log.debug("Created allowed user list : {}", (Object[]) allowedUsersArray);
        } else {
            allowedUsers = null;
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RegisterMessage message) throws Exception {
        String[] messageParts = StringUtils.split3(message.body);

        //expecting message with 2 parts at least.
        if (messageParts.length < 2) {
            log.error("Register Handler. Wrong income message format. {}", message);
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
            return;
        }

        String email = messageParts[0].trim().toLowerCase();
        String pass = messageParts[1];
        String appName = messageParts.length == 3 ? messageParts[2] : AppNameUtil.BLYNK;
        log.info("Trying register user : {}, app : {}", email, appName);

        if (BlynkEmailValidator.isNotValidEmail(email)) {
            log.error("Register Handler. Wrong email: {}", email);
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
            return;
        }

        if (userDao.isUserExists(email, appName)) {
            log.warn("User with email {} already exists.", email);
            ctx.writeAndFlush(alreadyRegistered(message.id), ctx.voidPromise());
            return;
        }

        if (allowedUsers != null && !allowedUsers.contains(email)) {
            log.warn("User with email {} not allowed to register.", email);
            ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
            return;
        }

        User newUser = userDao.add(email, pass, appName);

        log.info("Registered {}.", email);

        //sending greeting email only for Blynk apps
        if (AppNameUtil.BLYNK.equals(appName)) {
            blockingIOProcessor.execute(() -> {
                try {
                    mailWrapper.sendWelcomeEmailForNewUser(email);
                } catch (Exception e) {
                    log.warn("Error sending greeting email for {}.", email);
                }
            });
        }

        createProjectForExportedApp(newUser, appName, message.id);

        ctx.pipeline().remove(this);
        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

    private void createProjectForExportedApp(User newUser, String appName, int msgId) {
        if (appName.equals(AppNameUtil.BLYNK)) {
            return;
        }

        User parentUser = null;
        App app = null;

        for (User user : userDao.users.values()) {
            app = user.profile.getAppById(appName);
            if (app != null) {
                parentUser = user;
                break;
            }
        }

        if (app == null) {
            log.error("Unable to find app with id {}", appName);
            return;
        }

        if (app.isMultiFace) {
            log.info("App supports multi faces. Skipping profile creation.");
            return;
        }

        int dashId = app.projectIds[0];
        DashBoard dash = parentUser.profile.getDashByIdOrThrow(dashId);

        //todo ugly, but quick. refactor
        DashBoard clonedDash = JsonParser.parseDashboard(JsonParser.toJsonRestrictiveDashboard(dash), msgId);

        clonedDash.id = 1;
        clonedDash.parentId = dash.parentId;
        clonedDash.createdAt = System.currentTimeMillis();
        clonedDash.updatedAt = clonedDash.createdAt;
        clonedDash.isActive = true;
        clonedDash.eraseValues();
        clonedDash.removeDevicesProvisionedFromDeviceTiles();

        clonedDash.addTimers(timerWorker, new UserKey(newUser));

        newUser.profile.dashBoards = new DashBoard[] {clonedDash};

        if (app.provisionType == ProvisionType.STATIC) {
            for (Device device : clonedDash.devices) {
                device.erase();
            }
        } else {
            for (Device device : clonedDash.devices) {
                device.erase();
                String token = TokenGeneratorUtil.generateNewToken();
                tokenManager.assignToken(newUser, clonedDash, device, token);
            }
        }
    }

}
