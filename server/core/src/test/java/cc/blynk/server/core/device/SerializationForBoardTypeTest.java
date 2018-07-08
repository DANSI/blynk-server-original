package cc.blynk.server.core.device;

import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.serialization.JsonParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SerializationForBoardTypeTest {

    @Test
    public void someTEst() throws Exception {
        assertEquals("\"Arduino UNO\"", JsonParser.MAPPER.writeValueAsString(BoardType.Arduino_UNO));
        assertEquals(BoardType.Arduino_UNO, JsonParser.MAPPER.readValue("\"Arduino UNO\"", BoardType.class));
    }

    @Test
    public void testUnknownProperty() throws Exception {
        assertEquals(BoardType.Generic_Board, JsonParser.MAPPER.readValue("\"\"", BoardType.class));
        assertEquals(BoardType.Generic_Board, JsonParser.MAPPER.readValue("\"aaaa\"", BoardType.class));
    }

}
