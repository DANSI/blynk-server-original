package cc.blynk.server.core.model.serialization;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.Tag;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * Used to deep copy objects via Jackson serialization
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.09.18.
 */
public final class CopyUtil {

    private static final Logger log = LogManager.getLogger(CopyUtil.class);

    private CopyUtil() {
    }

    public static Tag[] copyTags(Tag[] tagsToCopy) {
        if (tagsToCopy.length == 0) {
            return tagsToCopy;
        }
        Tag[] copy = new Tag[tagsToCopy.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = tagsToCopy[i].copy();
        }
        return copy;
    }

    public static DashBoard deepCopy(DashBoard dash) {
        if (dash == null) {
            return null;
        }
        try {
            TokenBuffer tb = new TokenBuffer(JsonParser.MAPPER, false);
            JsonParser.MAPPER.writeValue(tb, dash);
            return JsonParser.MAPPER.readValue(tb.asParser(), DashBoard.class);
        } catch (Exception e) {
            log.error("Error during deep copy of dashboard. Reason : {}", e.getMessage());
            log.debug(e);
        }
        return null;
    }

    public static Profile deepCopy(Profile profile) {
        if (profile == null) {
            return null;
        }
        try {
            TokenBuffer tb = new TokenBuffer(JsonParser.MAPPER, false);
            JsonParser.MAPPER.writeValue(tb, profile);
            return JsonParser.MAPPER.readValue(tb.asParser(), Profile.class);
        } catch (Exception e) {
            log.error("Error during deep copy of profile. Reason : {}", e.getMessage());
            log.debug(e);
        }
        return null;
    }
}
