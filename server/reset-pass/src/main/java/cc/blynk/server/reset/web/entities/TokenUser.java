package cc.blynk.server.reset.web.entities;

/**
 * The Blynk project
 * Created by Andrew Zakordonets
 * Date : 12/05/2015.
 */
public class TokenUser {
    private final String email;
    private String newPassword;
    private long resetPasswordTokenTs;

    public TokenUser(String email) {
        this.email = email;
        this.newPassword = "";
        this.resetPasswordTokenTs = System.currentTimeMillis();
    }

    public String getEmail() {
        return email;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public long getResetPasswordTokenTs() {
        return resetPasswordTokenTs;
    }

    public void setResetPasswordTokenTs(long resetPasswordTokenTs) {
        this.resetPasswordTokenTs = resetPasswordTokenTs;
    }
}
