package cc.blynk.server.utils;

import java.util.regex.Pattern;

/**
 * User: ddumanskiy
 * Date: 8/11/13
 * Time: 6:34 PM
 */
public class EMailValidator {

    //from here : http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/
    private static final Pattern ptr = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

    public static boolean isValid(String email) {
        return ptr.matcher(email).matches();
    }

}
