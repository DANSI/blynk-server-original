package cc.blynk.server.core.exceptions;

import cc.blynk.common.enums.Response;
import cc.blynk.common.exceptions.BaseServerException;

public class QuotaLimitException extends BaseServerException {
	public QuotaLimitException(String message, int msgId) {
		super(message, msgId, Response.QUOTA_LIMIT_EXCEPTION);
	}
}
