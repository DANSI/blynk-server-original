package cc.blynk.test.utils;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Button;
import cc.blynk.server.core.model.widgets.controls.RGB;
import cc.blynk.server.core.model.widgets.controls.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 13:27
 */
public class JsonParsingTest {

    private static final ObjectReader profileReader = JsonParser.init().readerFor(Profile.class);

    public static Profile parseProfile(InputStream reader) {
        try {
            return profileReader.readValue(reader);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing profile");
        }
    }

    @Test
    public void testParseUserProfile() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_profile_json.txt");

        Profile profile = parseProfile(is);
        assertNotNull(profile);
        assertNotNull(profile.dashBoards);
        assertEquals(profile.dashBoards.length, 1);

        DashBoard dashBoard = profile.dashBoards[0];

        assertNotNull(dashBoard);

        assertEquals(1, dashBoard.id);
        assertEquals("My Dashboard", dashBoard.name);
        assertNotNull(dashBoard.widgets);
        assertEquals(dashBoard.widgets.length, 8);

        for (Widget widget : dashBoard.widgets) {
            assertNotNull(widget);
            assertEquals(1, widget.x);
            assertEquals(1, widget.y);
            assertEquals(1, widget.id);
            assertEquals("Some Text", widget.label);
        }
    }

    @Test
    public void testUserProfileToJson() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_profile_json.txt");

        Profile profile = parseProfile(is);
        String userProfileString = profile.toString();

        assertNotNull(userProfileString);
        assertTrue(userProfileString.contains("dashBoards"));
    }

    @Test
    public void testParseIOSProfile() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_ios_profile_json.txt");

        Profile profile = parseProfile(is);

        assertNotNull(profile);
        assertNotNull(profile.dashBoards);
        assertEquals(1, profile.dashBoards.length);
        assertNotNull(profile.dashBoards[0].widgets);
        assertNotNull(profile.dashBoards[0].widgets[0]);
        assertNotNull(profile.dashBoards[0].widgets[1]);
        assertTrue(((Button) profile.dashBoards[0].widgets[0]).pushMode);
        assertFalse(((Button) profile.dashBoards[0].widgets[1]).pushMode);
    }

    @Test
    public void testJoystickAndFieldAreParsed() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_profile_json_with_outdated_widget.txt");

        Profile profile = parseProfile(is);

        assertNotNull(profile);
        assertNotNull(profile.dashBoards);
        assertEquals(1, profile.dashBoards.length);
        assertNotNull(profile.dashBoards[0].widgets);
        assertNotNull(profile.dashBoards[0].widgets[0]);
        assertNotNull(profile.dashBoards[0].widgets[1]);
        assertTrue(profile.dashBoards[0].widgets[0] instanceof Button);
        assertTrue(profile.dashBoards[0].widgets[1] instanceof Button);
    }

    @Test
    public void testJSONToRGB() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_profile_json_RGB.txt");

        Profile profile = parseProfile(is);

        assertNotNull(profile);
        assertNotNull(profile.dashBoards);
        assertEquals(1, profile.dashBoards.length);
        assertNotNull(profile.dashBoards[0]);
        assertNotNull(profile.dashBoards[0].widgets);
        assertEquals(1, profile.dashBoards[0].widgets.length);

        RGB rgb = (RGB) profile.dashBoards[0].widgets[0];

        assertNotNull(rgb.dataStreams);
        assertEquals(2, rgb.dataStreams.length);
        DataStream dataStream1 = rgb.dataStreams[0];
        DataStream dataStream2 = rgb.dataStreams[1];

        assertNotNull(dataStream1);
        assertNotNull(dataStream2);

        assertEquals(1, dataStream1.pin);
        assertEquals(2, dataStream2.pin);

        assertEquals("1", dataStream1.value);
        assertEquals("2", dataStream2.value);

        assertEquals(PinType.DIGITAL, dataStream1.pinType);
        assertEquals(PinType.DIGITAL, dataStream2.pinType);

        assertFalse(dataStream1.pwmMode);
        assertTrue(dataStream2.pwmMode);

    }

    @Test
    public void testUserProfileToJson2() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_profile_json_2.txt");

        Profile profile = parseProfile(is);
        String userProfileString = profile.toString();

        assertNotNull(userProfileString);
        assertTrue(userProfileString.contains("dashBoards"));
    }

    @Test
    public void testUserProfileToJson3() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_profile_json_3.txt");

        Profile profile = parseProfile(is);
        String userProfileString = profile.toString();

        assertNotNull(userProfileString);
        assertTrue(userProfileString.contains("dashBoards"));
    }

    @Test
    public void testUserProfileToJsonWithTimer() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_profile_with_timer.txt");

        Profile profile = parseProfile(is);
        String userProfileString = profile.toString();

        assertNotNull(userProfileString);
        assertTrue(userProfileString.contains("dashBoards"));
        List<Timer> timers = getActiveTimerWidgets(profile);
        assertNotNull(timers);
        assertEquals(1, timers.size());
    }

    private List<Timer> getActiveTimerWidgets(Profile profile) {
        if (profile.dashBoards.length == 0) {
            return Collections.emptyList();
        }

        List<Timer> activeTimers = new ArrayList<>();
        for (DashBoard dashBoard : profile.dashBoards) {
            if (dashBoard.isActive) {
                activeTimers.addAll(getTimerWidgets(dashBoard));
            }
        }
        return activeTimers;
    }

    private List<Timer> getTimerWidgets(DashBoard dashBoard) {
        if (dashBoard.widgets.length == 0) {
            return Collections.emptyList();
        }

        List<Timer> timerWidgets = new ArrayList<>();
        for (Widget widget : dashBoard.widgets) {
            if (widget instanceof Timer) {
                Timer timer = (Timer) widget;
                if ((timer.startTime != -1 && timer.startValue != null && !timer.startValue.isEmpty()) ||
                        (timer.stopTime != -1 && timer.stopValue != null && !timer.stopValue.isEmpty())) {
                    timerWidgets.add(timer);
                }
            }
        }

        return timerWidgets;
    }


    @Test
    public void correctSerializedObject() throws JsonProcessingException {
        Button button = new Button();
        button.id = 1;
        button.label = "MyButton";
        button.x = 2;
        button.y = 2;
        button.color = 0;
        button.width = 2;
        button.height = 2;
        button.pushMode = false;

        String result = JsonParser.MAPPER.writeValueAsString(button);

        assertEquals("{\"type\":\"BUTTON\",\"id\":1,\"x\":2,\"y\":2,\"color\":0,\"width\":2,\"height\":2,\"tabId\":0,\"label\":\"MyButton\",\"isDefaultColor\":false,\"deviceId\":0,\"pin\":-1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0,\"pushMode\":false}", result);
    }
}
