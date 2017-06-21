package cc.blynk.server.core.model;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.controls.Button;
import cc.blynk.server.core.model.widgets.controls.RGB;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import java.io.InputStream;

import static cc.blynk.utils.StringUtils.split3;
import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.11.15.
 */
public class PinValuesUpdateCorrectTest {

    private static final ObjectReader profileReader = JsonParser.init().readerFor(Profile.class);

    public static Profile parseProfile(InputStream reader) {
        try {
            return profileReader.readValue(reader);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing profile");
        }
    }

    @Test
    public void testHas1Pin() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_profile_json.txt");

        Profile profile = parseProfile(is);
        DashBoard dash = profile.dashBoards[0];
        dash.isActive = true;

        Button button = dash.getWidgetByType(Button.class);
        assertEquals(1, button.pin);
        assertEquals(PinType.DIGITAL, button.pinType);
        assertEquals("1", button.value);

        update(dash, 0, "dw 1 0".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));
        assertEquals("0", button.value);

        update(dash, 0, "aw 1 1".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));
        assertEquals("0", button.value);

        update(dash, 0, "dw 1 1".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));
        assertEquals("1", button.value);

        RGB rgb = new RGB();
        rgb.pins = new Pin[3];
        rgb.pins[0] = new Pin((byte)0, false, false, PinType.VIRTUAL, null, 0, 255, null);
        rgb.pins[1] = new Pin((byte)1, false, false, PinType.VIRTUAL, null, 0, 255, null);
        rgb.pins[2] = new Pin((byte)2, false, false, PinType.VIRTUAL, null, 0, 255, null);


        dash.widgets = ArrayUtils.add(dash.widgets, rgb);

        update(dash, 0, "vw 0 100".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));
        update(dash, 0, "vw 1 101".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));
        update(dash, 0, "vw 2 102".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));

        for (int i = 0; i < rgb.pins.length; i++) {
            assertEquals("10" + i, rgb.pins[i].value);
        }

        rgb = new RGB();
        rgb.pins = new Pin[3];
        rgb.pins[0] = new Pin((byte)4, false, false, PinType.VIRTUAL, null, 0, 255, null);
        rgb.pins[1] = new Pin((byte)4, false, false, PinType.VIRTUAL, null, 0, 255, null);
        rgb.pins[2] = new Pin((byte)4, false, false, PinType.VIRTUAL, null, 0, 255, null);

        dash.widgets = ArrayUtils.add(dash.widgets, rgb);

        update(dash, 0, "vw 4 100 101 102".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));

        assertEquals("100 101 102".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING), rgb.pins[0].value);
    }

    public static void update(DashBoard dash, int deviceId, String body) {
        update(dash, deviceId, split3(body));
    }

    public static void update(DashBoard dash, int deviceId, String[] splitted) {
        final PinType type = PinType.getPinType(splitted[0].charAt(0));
        final byte pin = ParseUtil.parseByte(splitted[1]);
        dash.update(deviceId, pin, type, splitted[2], System.currentTimeMillis());
    }

}
