package cc.blynk.server.core.exceptions;

import cc.blynk.common.enums.Response;
import cc.blynk.common.exceptions.BaseServerException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/23/2015.
 */
public class NoActiveDashboardException extends BaseServerException {

    public NoActiveDashboardException(int msgId) {
        super("No active dashboard.", msgId, Response.NO_ACTIVE_DASHBOARD);
    }

}
