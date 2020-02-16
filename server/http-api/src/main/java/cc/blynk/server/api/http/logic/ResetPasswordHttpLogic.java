package cc.blynk.server.api.http.logic;

import cc.blynk.core.http.BaseHttpHandler;
import cc.blynk.core.http.Response;
import cc.blynk.core.http.annotation.Consumes;
import cc.blynk.core.http.annotation.Context;
import cc.blynk.core.http.annotation.FormParam;
import cc.blynk.core.http.annotation.GET;
import cc.blynk.core.http.annotation.Metric;
import cc.blynk.core.http.annotation.POST;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.core.http.annotation.PathParam;
import cc.blynk.core.http.annotation.QueryParam;
import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.internal.token.BaseToken;
import cc.blynk.server.internal.token.ResetPassToken;
import cc.blynk.server.internal.token.TokensPool;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.FileLoaderUtil;
import cc.blynk.utils.TokenGeneratorUtil;
import cc.blynk.utils.http.MediaType;
import cc.blynk.utils.properties.Placeholders;
import cc.blynk.utils.validators.BlynkEmailValidator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.core.http.Response.badRequest;
import static cc.blynk.core.http.Response.noResponse;
import static cc.blynk.core.http.Response.notFound;
import static cc.blynk.core.http.Response.ok;
import static cc.blynk.core.http.Response.serverError;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_CLONE;
import static cc.blynk.utils.http.MediaType.TEXT_PLAIN;

/**
 * The Blynk project
 * Created by Andrew Zakordonets
 * Date : 12/05/2015.
 */
@Path("/")
@ChannelHandler.Sharable
public class ResetPasswordHttpLogic extends BaseHttpHandler {

    private static final Logger log = LogManager.getLogger(ResetPasswordHttpLogic.class);

    private final UserDao userDao;
    private final TokensPool tokensPool;
    private final String emailBody;
    private final String emailSubj;
    private final MailWrapper mailWrapper;
    private final String resetPassUrl;
    private final String pageContent;
    private final String newResetPage;
    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;
    private final FileManager fileManager;
    private final String resetClickHost;

    public ResetPasswordHttpLogic(Holder holder) {
        super(holder, "");
        this.userDao = holder.userDao;
        this.tokensPool = holder.tokensPool;
        String productName = holder.props.productName;
        this.emailSubj = "Password reset request for the " + productName + " app.";
        this.emailBody = FileLoaderUtil.readResetEmailTemplateAsString()
                .replace(Placeholders.PRODUCT_NAME, productName);
        this.newResetPage = holder.textHolder.appResetEmailTemplate
                .replace(Placeholders.PRODUCT_NAME, productName);
        this.mailWrapper = holder.mailWrapper;

        String host = holder.props.host;

        //using https for private servers as they have valid certificates.
        String protocol = host.endsWith(".blynk.cc") ? "https://" : "http://";
        this.resetPassUrl = protocol + host + "/landing?token=";
        this.pageContent = holder.textHolder.resetPassLandingTemplate;
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.dbManager = holder.dbManager;
        this.fileManager = holder.fileManager;
        this.resetClickHost = holder.props.getRestoreHost();
    }

    private static String generateToken() {
        return TokenGeneratorUtil.generateNewToken() + TokenGeneratorUtil.generateNewToken();
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Path("resetPassword")
    public Response sendResetPasswordEmail(@Context ChannelHandlerContext ctx,
                                           @FormParam("email") String email,
                                           @FormParam("appName") String appName) {

        if (BlynkEmailValidator.isNotValidEmail(email)) {
            return badRequest(email + " email has not valid format.");
        }

        String trimmedEmail = email.trim().toLowerCase();
        appName = (appName == null ? AppNameUtil.BLYNK : appName);

        User user = userDao.getByName(trimmedEmail, appName);

        if (user == null) {
            return badRequest("Sorry, this account does not exist.");
        }

        String token = generateToken();
        log.info("{} trying to reset pass.", trimmedEmail);

        ResetPassToken userToken = new ResetPassToken(trimmedEmail, appName);
        tokensPool.addToken(token, userToken);
        String message = emailBody.replace(Placeholders.RESET_URL, resetPassUrl + token);
        log.info("Sending token to {} address", trimmedEmail);

        blockingIOProcessor.execute(() -> {
            Response response;
            try {
                mailWrapper.sendHtml(trimmedEmail, emailSubj, message);
                log.info("{} mail sent.", trimmedEmail);
                response = ok("Email was sent.");
            } catch (Exception e) {
                log.info("Error sending mail for {}. Reason : {}", trimmedEmail, e.getMessage());
                response = badRequest("Error sending reset email.");
            }
            if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                ctx.writeAndFlush(response, ctx.voidPromise());
            }
        });

        return noResponse();
    }

    @GET
    @Path("landing")
    public Response generateResetPage(@QueryParam("token") String token) {
        BaseToken baseToken = tokensPool.getBaseToken(token);
        if (baseToken == null) {
            return badRequest("Your token was not found or it is outdated. Please try again.");
        }

        if (TokenGeneratorUtil.isNotValidResetToken(token)) {
            return badRequest("Invalid request parameters.");
        }

        log.info("{} landed.", baseToken.email);
        String page = pageContent.replace(Placeholders.EMAIL, baseToken.email).replace(Placeholders.TOKEN, token);
        return ok(page, MediaType.TEXT_HTML);
    }

    @GET
    @Path("restore")
    public Response getNewResetPage(@QueryParam("token") String token, @QueryParam("email") String email) {
        //we do not check token here, as we used single host but we may have many servers

        //ResetPassToken user = tokensPool.getBaseToken(token);
        //if (user == null) {
        //    return badRequest("Your token was not found or it is outdated. Please try again.");
        //}

        if (TokenGeneratorUtil.isNotValidResetToken(token)
                || (email != null && BlynkEmailValidator.isNotValidEmail(email))) {
            return badRequest("Invalid request parameters.");
        }

        log.info("{} landed.", email);
        String resetUrl = "http://" + resetClickHost + "/restore?token=" + token + "&email=" + email;
        String body = newResetPage.replace(Placeholders.RESET_URL, resetUrl);
        return ok(body, MediaType.TEXT_HTML);
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Path("updatePassword")
    public Response updatePassword(@FormParam("password") String passHash,
                                   @FormParam("token") String token) {
        ResetPassToken resetPassToken = tokensPool.getResetPassToken(token);
        if (resetPassToken == null) {
            return badRequest("Invalid token. Please repeat all steps.");
        }

        log.info("Resetting pass for {}", resetPassToken.email);
        User user = userDao.getByName(resetPassToken.email, resetPassToken.appName);

        if (user == null) {
            log.warn("No user with email {}", resetPassToken.email);
            return notFound();
        }

        user.resetPass(passHash);

        log.info("{} password was reset.", user.email);
        tokensPool.removeToken(token);
        return ok("Password was successfully reset.", TEXT_PLAIN);
    }

    @GET
    @Path("{token}/clone")
    @Metric(HTTP_CLONE)
    public Response getClone(@Context ChannelHandlerContext ctx,
                             @PathParam("token") String token) {

        blockingIOProcessor.executeDB(() -> {
            try {
                String json = dbManager.selectClonedProject(token);
                //no cloned project in DB, checking local storage on disk
                if (json == null) {
                    json = fileManager.readClonedProjectFromDisk(token);
                }
                if (json == null) {
                    log.debug("Requested QR not found. {}", token);
                    ctx.writeAndFlush(serverError("Requested QR not found."), ctx.voidPromise());
                } else {
                    ctx.writeAndFlush(ok(json), ctx.voidPromise());
                }
            } catch (Exception e) {
                log.error("Error cloning project.", e);
                ctx.writeAndFlush(serverError("Error getting cloned project."), ctx.voidPromise());
            }
        });

        return null;
    }

}
