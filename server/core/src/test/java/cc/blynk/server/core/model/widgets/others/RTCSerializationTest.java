package cc.blynk.server.core.model.widgets.others;

import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.others.rtc.RTC;
import cc.blynk.utils.DateTimeUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 30.03.16.
 */
public class RTCSerializationTest {

    @Test
    public void testDeSerializationIsCorrect() {
        String widgetString = "{\"id\":1, \"x\":1, \"y\":1, \"type\":\"RTC\", \"tzName\":\"Australia/Sydney\"}";
        Widget widget = JsonParser.parseWidget(widgetString, 0);

        assertNotNull(widget);

        RTC rtc = (RTC) widget;
        assertNotNull(rtc.tzName);
        assertEquals(ZoneId.of("Australia/Sydney"), rtc.tzName);
    }

    @Test
    @Ignore("travis uses old java and fails here")
    public void unsupportedTimeZoneForKnownLocationCanadaTest() {
        String widgetString = "{\"id\":1, \"x\":1, \"y\":1, \"type\":\"RTC\", \"tzName\":\"Canada/East-Saskatchewan\"}";
        Widget widget = JsonParser.parseWidget(widgetString, 0);

        assertNotNull(widget);

        RTC rtc = (RTC) widget;
        assertNotNull(rtc.tzName);
        assertEquals(ZoneId.of("America/Regina"), rtc.tzName);
    }

    @Test
    public void unsupportedTimeZoneForKnownLocationHanoiTest() {
        String widgetString = "{\"id\":1, \"x\":1, \"y\":1, \"type\":\"RTC\", \"tzName\":\"Asia/Hanoi\"}";
        Widget widget = JsonParser.parseWidget(widgetString, 0);

        assertNotNull(widget);

        RTC rtc = (RTC) widget;
        assertNotNull(rtc.tzName);
        assertEquals(ZoneId.of("Asia/Ho_Chi_Minh"), rtc.tzName);
    }

    @Test
    public void unsupportedTimeZoneTest() {
        String widgetString = "{\"id\":1, \"x\":1, \"y\":1, \"type\":\"RTC\", \"tzName\":\"Canada/East-xxx\"}";
        RTC rtc = (RTC) JsonParser.parseWidget(widgetString, 0);
        assertNotNull(rtc);
        assertEquals(ZoneId.of("UTC"), rtc.tzName);
    }

    @Test
    public void testDeSerializationIsCorrectForNull() {
        String widgetString = "{\"id\":1, \"x\":1, \"y\":1, \"type\":\"RTC\"}";
        Widget widget = JsonParser.parseWidget(widgetString, 0);

        assertNotNull(widget);

        RTC rtc = (RTC) widget;
        assertNull(rtc.tzName);
    }

    @Test
    public void testSerializationIsCorrect() throws Exception {
        RTC rtc = new RTC();
        rtc.tzName = ZoneId.of("Australia/Sydney");

        String widgetString = JsonParser.MAPPER.writeValueAsString(rtc);

        assertNotNull(widgetString);
        assertEquals("{\"type\":\"RTC\",\"id\":0,\"x\":0,\"y\":0,\"color\":0,\"width\":0,\"height\":0,\"tabId\":0,\"isDefaultColor\":false,\"tzName\":\"Australia/Sydney\"}", widgetString);
    }

    @Test
    public void testSerializationIsCorrectUTC() throws Exception {
        RTC rtc = new RTC();
        rtc.tzName = DateTimeUtils.UTC;

        String widgetString = JsonParser.MAPPER.writeValueAsString(rtc);

        assertNotNull(widgetString);
        assertEquals("{\"type\":\"RTC\",\"id\":0,\"x\":0,\"y\":0,\"color\":0,\"width\":0,\"height\":0,\"tabId\":0,\"isDefaultColor\":false,\"tzName\":\"UTC\"}", widgetString);
    }

    @Test
    public void testSerializationIsCorrectForNull() throws Exception {
        RTC rtc = new RTC();
        rtc.tzName = null;

        String widgetString = JsonParser.MAPPER.writeValueAsString(rtc);

        assertNotNull(widgetString);
        assertEquals("{\"type\":\"RTC\",\"id\":0,\"x\":0,\"y\":0,\"color\":0,\"width\":0,\"height\":0,\"tabId\":0,\"isDefaultColor\":false}", widgetString);
    }

}
