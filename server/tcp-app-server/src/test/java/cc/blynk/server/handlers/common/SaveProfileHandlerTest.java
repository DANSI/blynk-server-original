package cc.blynk.server.handlers.common;

import cc.blynk.common.model.messages.protocol.appllication.SaveProfileMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.exceptions.IllegalCommandBodyException;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.handlers.app.main.logic.SaveProfileLogic;
import org.junit.Test;

import static cc.blynk.common.enums.Command.*;
import static cc.blynk.common.model.messages.MessageFactory.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/26/2015.
 */
public class SaveProfileHandlerTest {

    ServerProperties props = new ServerProperties();

    private SaveProfileLogic saveProfileHandler = new SaveProfileLogic(props);

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
