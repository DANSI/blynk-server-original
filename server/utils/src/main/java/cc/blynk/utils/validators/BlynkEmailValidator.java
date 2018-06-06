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

    public static boolean isValidEmail(String email) {
        return !isNotValidEmail(email);
    }

}
