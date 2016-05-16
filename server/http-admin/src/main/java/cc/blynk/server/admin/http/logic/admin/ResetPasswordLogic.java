package cc.blynk.server.admin.http.logic.admin;

import cc.blynk.server.admin.http.pojo.TokenUser;
import cc.blynk.server.admin.http.pojo.TokensPool;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.handlers.http.rest.Response;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.utils.FileLoaderUtil;
import cc.blynk.utils.IPUtils;
import cc.blynk.utils.ServerProperties;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.net.URISyntaxException;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * The Blynk project
 * Created by Andrew Zakordonets
 * Date : 12/05/2015.
 */
@Path("/")
public class ResetPasswordLogic {

    private static final Logger log = LogManager.getLogger(ResetPasswordLogic.class);

    private final UserDao userDao;
    private final TokensPool tokensPool;
    private final String emailBody;
    private final MailWrapper mailWrapper;
    private final String resetPassUrl;
    private final String pageContent;

    public static final String RESET_PASS_STATIC_PATH = "static/reset/";

    public ResetPasswordLogic(ServerProperties props, UserDao userDao, MailWrapper mailWrapper) {
        this.userDao = userDao;
        this.tokensPool = new TokensPool();
        this.emailBody = FileLoaderUtil.readFileAsString(RESET_PASS_STATIC_PATH + "reset-email.html");
        this.mailWrapper = mailWrapper;

        this.resetPassUrl = String.format("http://%s:%s/landing?token=",
                props.getProperty("reset-pass.http.host", IPUtils.resolveHostIP()),
                props.getIntProperty("reset.pass.http.port")
        );
        this.pageContent = FileLoaderUtil.readFileAsString(RESET_PASS_STATIC_PATH + "enterNewPassword.html");
    }

    private static String generateToken() {
        return (UUID.randomUUID().toString() + UUID.randomUUID().toString()).replace("-", "");
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Path("resetPassword")
    public Response sendResetPasswordEmail(@FormParam("email") String email) {
        if (email == null || email.isEmpty()) {
            return Response.badRequest("Email field is empty. Please input your email.");
        }

        if (!EmailValidator.getInstance().isValid(email)) {
            return Response.badRequest(String.format("%s email has not valid format.", email));
        }

        String token = generateToken();
        log.info("{} trying to reset pass.", email);
        try {
            TokenUser user = new TokenUser(email);
            tokensPool.addToken(token, user);
            String message = emailBody.replace("{RESET_URL}", resetPassUrl + token);
            log.info("Sending token to {} address", email);
            mailWrapper.send(email, "Password reset request for Blynk app.", message, "text/html");
        } catch (Exception e) {
            log.info("Error sending mail for {}", email, e);
            return Response.badRequest("Error sending reset email.");
        }
        log.info("{} mail sent.", email);
        return Response.ok("Email was sent");
    }

    @GET
    @Path("landing")
    public Response generateResetPage(@QueryParam("token") String token) throws URISyntaxException {
        TokenUser user = tokensPool.getUser(token);
        if (user == null) {
            return Response.badRequest("Your token was not found or it is outdated. Please try again.");
        }

        log.info("{} landed.", user.getEmail());
        String page = pageContent.replace("{EMAIL}", user.getEmail()).replace("{TOKEN}", token);
        return Response.ok(page, MediaType.TEXT_HTML);
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Path("updatePassword")
    public Response updatePassword(@FormParam("password") String password,
                                   @FormParam("token") String token) {
        TokenUser tokenUser = tokensPool.getUser(token);
        if (tokenUser == null) {
            return Response.badRequest("Invalid token. Please repeat all steps.");
        }

        log.info("Resetting pass for {}", tokenUser.getEmail());
        User user = userDao.getByName(tokenUser.getEmail());

        if (user == null) {
            log.warn("No user with name {}", tokenUser.getEmail());
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        user.pass = password;
        user.lastModifiedTs = System.currentTimeMillis();

        log.info("{} password was reset.", user.name);
        tokensPool.removeToken(token);
        return Response.ok("Password was successfully reset.");
    }

}
