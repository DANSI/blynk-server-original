package cc.blynk.server.application.handlers.main.auth;

import org.apache.commons.validator.routines.EmailValidator;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.17.
 */
public class BlynkEmailValidator {

    public static boolean isNotValidEmail(String email) {
        return email == null || email.isEmpty() || email.length() > 255 ||
                email.contains("?") ||
                !EmailValidator.getInstance().isValid(email);
    }

}
