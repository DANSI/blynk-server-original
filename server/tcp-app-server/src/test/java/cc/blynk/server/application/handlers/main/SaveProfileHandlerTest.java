package cc.blynk.server.application.handlers.main;

import cc.blynk.server.application.handlers.main.logic.SaveProfileLogic;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.appllication.SaveProfileMessage;
import cc.blynk.utils.ServerProperties;
import org.junit.Test;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/26/2015.
 */
public class SaveProfileHandlerTest {

    ServerProperties props = new ServerProperties();

    private SaveProfileLogic saveProfileHandler = new SaveProfileLogic(10, props.getIntProperty("user.profile.max.size") * 1024);

    @Test(expected = NotAllowedException.class)
    public void testTooBigUserProfile() throws Exception {
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < props.getIntProperty("user.profile.max.size") * 1024 + 1; i++) {
            tmp.append('a');
        }

        SaveProfileMessage msg = (SaveProfileMessage) produce(1, SAVE_PROFILE, tmp.toString());
        saveProfileHandler.messageReceived(null, null, msg);
    }

    @Test(expected = IllegalCommandBodyException.class)
    public void testIllegalProfile() throws Exception {
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < props.getIntProperty("user.profile.max.size") * 1024; i++) {
            tmp.append('a');
        }

        SaveProfileMessage msg = (SaveProfileMessage) produce(1, SAVE_PROFILE, tmp.toString());
        saveProfileHandler.messageReceived(null, null, msg);
    }

}
