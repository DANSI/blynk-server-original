package cc.blynk.server.core.model;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.controls.Button;
import cc.blynk.server.core.model.widgets.controls.RGB;
import cc.blynk.utils.NumberUtil;
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
public class DataStreamValuesUpdateCorrectTest {

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

        update(profile, 0, "dw 1 0".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));
        assertEquals("0", button.value);

        update(profile, 0, "aw 1 1".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));
        assertEquals("0", button.value);

        update(profile, 0, "dw 1 1".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));
        assertEquals("1", button.value);

        RGB rgb = new RGB();
        rgb.dataStreams = new DataStream[3];
        rgb.dataStreams[0] = new DataStream((short) 0, false, false, PinType.VIRTUAL, null, 0, 255, null);
        rgb.dataStreams[1] = new DataStream((short) 1, false, false, PinType.VIRTUAL, null, 0, 255, null);
        rgb.dataStreams[2] = new DataStream((short) 2, false, false, PinType.VIRTUAL, null, 0, 255, null);


        dash.widgets = ArrayUtils.add(dash.widgets, rgb);

        update(profile, 0, "vw 0 100".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));
        update(profile, 0, "vw 1 101".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));
        update(profile, 0, "vw 2 102".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));

        for (int i = 0; i < rgb.dataStreams.length; i++) {
            assertEquals("10" + i, rgb.dataStreams[i].value);
        }

        rgb = new RGB();
        rgb.dataStreams = new DataStream[3];
        rgb.dataStreams[0] = new DataStream((short) 4, false, false, PinType.VIRTUAL, null, 0, 255, null);
        rgb.dataStreams[1] = new DataStream((short) 4, false, false, PinType.VIRTUAL, null, 0, 255, null);
        rgb.dataStreams[2] = new DataStream((short) 4, false, false, PinType.VIRTUAL, null, 0, 255, null);

        dash.widgets = ArrayUtils.add(dash.widgets, rgb);

        update(profile, 0, "vw 4 100 101 102".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));

        assertEquals("100 101 102".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING), rgb.dataStreams[0].value);
    }

    public static void update(Profile profile, int deviceId, String body) {
        update(profile, deviceId, split3(body));
    }

    public static void update(Profile profile, int deviceId, String[] splitted) {
        final PinType type = PinType.getPinType(splitted[0].charAt(0));
        final short pin = NumberUtil.parsePin(splitted[1]);
        profile.update(profile.dashBoards[0], deviceId, pin, type, splitted[2], System.currentTimeMillis());
    }

}
