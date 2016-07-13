package cc.blynk.utils;

import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.LED;
import org.junit.Test;

import static cc.blynk.utils.ReflectionUtil.*;
import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.07.16.
 */
public class ReflectionUtilTest {

    @Test
    public void testSetPropertyViaReflection() throws Exception {
        Widget widget = new LED();

        setProperty(widget, "label", "newLabel");
        assertEquals("newLabel", widget.label);

        setProperty(widget, "frequency", "123");
        assertEquals(123, ((LED) widget).getFrequency());
    }

    @Test
    public void testSetPropertyViaReflectionFails() throws Exception {
        Widget widget = new LED();

        setProperty(widget, "aaa", "newLabel");
        assertEquals("newLabel", widget.label);
    }

}
