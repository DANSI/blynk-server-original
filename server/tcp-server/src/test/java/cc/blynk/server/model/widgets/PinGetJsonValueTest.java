package cc.blynk.server.model.widgets;

import cc.blynk.server.model.Pin;
import cc.blynk.server.model.widgets.controls.RGB;
import cc.blynk.server.model.widgets.outputs.Digit4Display;
import org.junit.Test;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.12.15.
 */
public class PinGetJsonValueTest {

    private static Pin createPinWithValue(String val) {
        Pin pin = new Pin();
        pin.value = val;
        return pin;
    }

    @Test
    public void testSinglePin() {
        OnePinWidget onePinWidget = new Digit4Display();
        onePinWidget.value = null;

        assertEquals("[]", onePinWidget.getJsonPinValues());

        onePinWidget.value = "1.0";
        assertEquals("[\"1.0\"]", onePinWidget.getJsonPinValues());
    }

    @Test
    public void testMultiPin() {
        MultiPinWidget multiPinWidget = new RGB();
        multiPinWidget.pins = null;

        assertEquals("[]", multiPinWidget.getJsonPinValues());

        multiPinWidget.pins = new Pin[3];
        multiPinWidget.pins[0] = createPinWithValue("1");
        multiPinWidget.pins[1] = createPinWithValue("2");
        multiPinWidget.pins[2] = createPinWithValue("3");

        assertEquals("[\"1\",\"2\",\"3\"]", multiPinWidget.getJsonPinValues());
    }

}
