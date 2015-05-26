package cc.blynk.server.reset.web.controller;

import cc.blynk.common.utils.Config;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.core.administration.AdminLauncher;
import cc.blynk.server.notifications.mail.MailSender;
import cc.blynk.server.reset.web.entities.TokenUser;
import cc.blynk.server.reset.web.entities.TokensPool;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;

/**
 * The Blynk project
 * Created by Andrew Zakordonets
 * Date : 12/15/2015.
 */
public class ResetPasswordController {

    private static final Logger log = LogManager.getLogger(ResetPasswordController.class);
    private final MailSender mailSender;
    private final String url;
    private final int port;
    private final TokensPool tokensPool;
    private final String body;
    private final String pageContent;

    public ResetPasswordController(String url, int port, TokensPool tokensPool) throws Exception {
        this(url, port, tokensPool, new MailSender(new ServerProperties(Config.MAIL_PROPERTIES_FILENAME)));
    }

    public ResetPasswordController(String url, int port, TokensPool tokensPool, MailSender mailSender) throws Exception {
        this.mailSender = mailSender;
        this.url = url;
        this.port = port;

        URL bodyUrl = Resources.getResource("body/message.html");
        this.body = Resources.toString(bodyUrl, Charsets.UTF_8);
        this.tokensPool = tokensPool;
        URL pageUrl = Resources.getResource("html/enterNewPassword.html");
        this.pageContent = Resources.toString(pageUrl, Charsets.UTF_8);
    }

    public void sendResetPasswordEmail(String email, String token) throws Exception {
        TokenUser user = new TokenUser(email);
        tokensPool.addToken(token, user);
        String resetUrl = String.format("%s%s/landing?token=%s", url, (port == 80) ? "" : ":" + port, token);
        String message = body.replace("{RESET_URL}", resetUrl);
        log.info("Sending token to {} address", email);
        mailSender.sendMail(email, "Password reset request for Blynk app.", message, "text/html");
    }

    public void invoke(String email, String password) throws IOException {
        new AdminLauncher("cloud.blynk.cc", 8777).connect("resetpassword", email, password);
    }

    public String getResetPasswordPage(String email, String token) {
        return pageContent.replace("{EMAIL}", email).replace("{TOKEN}", token);
    }
}
