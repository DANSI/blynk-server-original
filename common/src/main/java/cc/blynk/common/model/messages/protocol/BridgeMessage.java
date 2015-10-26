package cc.blynk.common.model.messages.protocol;

import cc.blynk.common.model.messages.StringMessage;

import static cc.blynk.common.enums.Command.BRIDGE;

/**
 * The Blynk Project.
 * Created by Andrew Zakordonets.
 * Created on 27/4/2015.
 */
public class BridgeMessage extends StringMessage {

	public BridgeMessage(int messageId, String body) {
		super(messageId, BRIDGE, body.length(), body);
	}

	@Override
	public String toString() {
		return "BridgeMessage{" + super.toString() + "}";
	}
}
