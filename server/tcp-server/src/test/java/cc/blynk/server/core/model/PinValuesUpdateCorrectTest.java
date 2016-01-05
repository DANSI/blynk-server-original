package cc.blynk.server.core.model;

import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.controls.Button;
import cc.blynk.server.core.model.widgets.controls.RGB;
import cc.blynk.utils.JsonParser;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.11.15.
 */
public class PinValuesUpdateCorrectTest {

    @Test
    public void testHas1Pin() {
        InputStream is = this.getClass().getResourceAsStream("/json_test/user_profile_json.txt");

        Profile profile = JsonParser.parseProfile(is);
        DashBoard dash = profile.dashBoards[0];
        dash.isActive = true;

        Button button = dash.getWidgetByType(Button.class);
        assertEquals(1, button.pin.byteValue());
        assertEquals(PinType.DIGITAL, button.pinType);
        assertEquals("1", button.value);

        dash.update(new HardwareBody("dw 1 0".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING), 0));
        assertEquals("0", button.value);

        dash.update(new HardwareBody("aw 1 1".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING), 0));
        assertEquals("0", button.value);

        dash.update(new HardwareBody("dw 1 1".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING), 0));
        assertEquals("1", button.value);

        RGB rgb = new RGB();
        rgb.pins = new Pin[3];
        rgb.pins[0] = new Pin();
        rgb.pins[0].pin = 0;
        rgb.pins[0].pinType = PinType.VIRTUAL;
        rgb.pins[1] = new Pin();
        rgb.pins[1].pin = 1;
        rgb.pins[1].pinType = PinType.VIRTUAL;
        rgb.pins[2] = new Pin();
        rgb.pins[2].pin = 2;
        rgb.pins[2].pinType = PinType.VIRTUAL;


        dash.widgets = ArrayUtils.add(dash.widgets, rgb);

        dash.update(new HardwareBody("vw 0 100".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING), 0));
        dash.update(new HardwareBody("vw 1 101".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING), 0));
        dash.update(new HardwareBody("vw 2 102".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING), 0));

        for (int i = 0; i < rgb.pins.length; i++) {
            assertEquals("10" + i, rgb.pins[i].value);
        }

        rgb = new RGB();
        rgb.pins = new Pin[3];
        rgb.pins[0] = new Pin();
        rgb.pins[0].pin = 4;
        rgb.pins[0].pinType = PinType.VIRTUAL;
        rgb.pins[1] = new Pin();
        rgb.pins[1].pin = 4;
        rgb.pins[1].pinType = PinType.VIRTUAL;
        rgb.pins[2] = new Pin();
        rgb.pins[2].pin = 4;
        rgb.pins[2].pinType = PinType.VIRTUAL;

        dash.widgets = ArrayUtils.add(dash.widgets, rgb);

        dash.update(new HardwareBody("vw 4 100 101 102".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING), 0));

        for (int i = 0; i < rgb.pins.length; i++) {
            assertEquals("10" + i, rgb.pins[i].value);
        }

    }


}
