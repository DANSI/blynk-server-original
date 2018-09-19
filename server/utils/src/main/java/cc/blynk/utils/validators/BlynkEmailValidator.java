package cc.blynk.utils.validators;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.17.
 */
public final class BlynkEmailValidator {

    private BlynkEmailValidator() {
    }

    public static boolean isNotValidEmail(String email) {
        return email == null || email.isEmpty() || email.length() > 255
                || email.contains("?") || !email.contains("@")
                || !EmailValidator.getInstance().isValid(email);
    }

    public static boolean isValidEmails(String emails) {
        if (emails == null || emails.isEmpty()) {
            return false;
        }
        String[] emailsSplit = emails.split(",");
        if (emailsSplit.length > 5) {
            return false;
        }
        for (String email : emailsSplit) {
            if (isNotValidEmail(email)) {
                return false;
            }
        }
        return true;
    }

}
