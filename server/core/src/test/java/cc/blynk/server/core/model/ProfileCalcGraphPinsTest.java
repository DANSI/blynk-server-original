package cc.blynk.server.core.model;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.graph.GraphKey;
import cc.blynk.utils.JsonParser;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/17/2015.
 */
public class ProfileCalcGraphPinsTest {

    @Test
    public void testHas1Pin() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_profile_json.txt");

        Profile profile = JsonParser.parseProfile(is);
        String userProfileString = profile.toString();

        assertNotNull(userProfileString);
        assertTrue(userProfileString.contains("dashBoards"));

        profile.calcGraphPins();

        assertTrue(profile.hasGraphPin(new GraphKey(1, (byte) 8, PinType.DIGITAL)));
    }

    @Test
    public void testNoPins() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_profile_json_4.txt");

        Profile profile = JsonParser.parseProfile(is);
        String userProfileString = profile.toString();

        assertNotNull(userProfileString);
        assertTrue(userProfileString.contains("dashBoards"));

        profile.calcGraphPins();

        assertFalse(profile.hasGraphPin(new GraphKey(1, (byte) 8, PinType.ANALOG)));
    }

    @Test
    public void testManyPins() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_profile_json_5.txt");

        Profile profile = JsonParser.parseProfile(is);
        String userProfileString = profile.toString();

        assertNotNull(userProfileString);
        assertTrue(userProfileString.contains("dashBoards"));

        profile.calcGraphPins();

        assertTrue(profile.hasGraphPin(new GraphKey(1, (byte) 8, PinType.DIGITAL)));
        assertTrue(profile.hasGraphPin(new GraphKey(1, (byte) 9, PinType.DIGITAL)));


        assertFalse(profile.hasGraphPin(new GraphKey(2, (byte) 9, PinType.DIGITAL)));
        assertTrue(profile.hasGraphPin(new GraphKey(2, (byte) 8, PinType.DIGITAL)));
        assertTrue(profile.hasGraphPin(new GraphKey(2, (byte) 2, PinType.DIGITAL)));
    }

}
