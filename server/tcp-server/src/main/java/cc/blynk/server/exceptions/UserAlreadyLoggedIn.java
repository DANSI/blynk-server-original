package cc.blynk.server.exceptions;

import cc.blynk.common.enums.Response;
import cc.blynk.common.exceptions.BaseServerException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public class UserAlreadyLoggedIn extends BaseServerException {

    public UserAlreadyLoggedIn(int msgId) {
        super("User already logged. Client problem. CHECK!", msgId, Response.USER_ALREADY_LOGGED_IN);
    }

}
