package cc.blynk.server.reset.web.handlers;

import cc.blynk.common.utils.EMailValidator;
import cc.blynk.server.reset.web.controller.ResetPasswordController;
import cc.blynk.server.reset.web.entities.TokenUser;
import cc.blynk.server.reset.web.entities.TokensPool;
import fabricator.Fabricator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * The Blynk project
 * Created by Andrew Zakordonets
 * Date : 12/05/2015.
 */
@Path("/")
public class ResetPasswordHandler {

    private static final Logger log = LogManager.getLogger(ResetPasswordHandler.class);

    private final ResetPasswordController resetPasswordController;
    private final TokensPool tokensPool;

    public ResetPasswordHandler(String url, int port, TokensPool tokensPool) throws Exception {
        this.resetPasswordController = new ResetPasswordController(url, port, tokensPool);
        this.tokensPool = tokensPool;
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Path("resetPassword")
    public Response sendResetPasswordEmail(@FormParam("email") String email) {
        if (email == null || email.isEmpty()) {
            return badRequestResponse("Email field is empty. Please input your email.");
        }

        if (!EMailValidator.isValid(email)) {
            return badRequestResponse(String.format("%s email has not valid format.", email));
        }

        String token = Fabricator.alphaNumeric().hash(60);
        log.info("{} trying to reset pass.", email);
        try {
            resetPasswordController.sendResetPasswordEmail(email, token);
        } catch (Exception e) {
            log.info("Error sending mail for {}", email);
            return badRequestResponse("Error sending reset email.");
        }
        log.info("{} mail sent.", email);
        return Response.ok().entity("Email was sent").build();
    }

    private Response badRequestResponse(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(message)
                .build();
    }

    @GET
    @Produces(value = MediaType.TEXT_HTML)
    @Path("landing")
    public Response generateResetPage(@QueryParam("token") String token) throws URISyntaxException {
        TokenUser user = tokensPool.getUser(token);
        if (user == null) {
            return notFoundResponse(String.format("%s token was not found or it is outdated", token));
        }

        log.info("{} landed.", user.getEmail());
        String page = resetPasswordController.getResetPasswordPage(user.getEmail(), token);
        return Response.ok(page).build();
    }

    @GET
    @Path("static/{filename}")
    @Produces("text/css")
    public Response getStatic(@PathParam("filename") String filename) throws URISyntaxException {
        InputStream is = ResetPasswordHandler.class.getClassLoader().getResourceAsStream("/html/" + filename);
        return Response.ok(is).build();
    }


    @POST
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(value = MediaType.APPLICATION_JSON)
    @Path("updatePassword")
    public Response updatePassword(@FormParam("password") String password,
                                   @FormParam("token") String token) {
        TokenUser user = tokensPool.getUser(token);
        if (user == null) {
            return notFoundResponse("Invalid token. Please repeat all steps.");
        }

        try {
            log.info("Resetting...");
            resetPasswordController.invoke(user.getEmail(), password);
            log.info("{} password was reset.", user.getEmail());
            tokensPool.removeToken(token);
            return Response.ok().build();
        } catch (IOException ioe) {
            log.error("Error resetting pass for {}.", user.getEmail());
            log.error(ioe);
            return badRequestResponse("Failed to reset pass.");
        }
    }

    private Response notFoundResponse(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(message)
                .build();
    }

}
