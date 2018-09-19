package cc.blynk.server.core.protocol.exceptions;

import cc.blynk.server.core.protocol.enums.Response;

public class QuotaLimitException extends BaseServerException {

    public QuotaLimitException(String message) {
        super(message, Response.QUOTA_LIMIT);
    }

    public QuotaLimitException(String message, int msgId) {
        super(message, msgId, Response.QUOTA_LIMIT);
    }

}
