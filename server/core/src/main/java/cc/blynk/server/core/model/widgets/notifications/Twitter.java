package cc.blynk.server.core.model.widgets.notifications;

import cc.blynk.server.core.model.serialization.View;
import cc.blynk.server.core.model.widgets.NoPinWidget;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Twitter extends NoPinWidget {

    private static final int MAX_TWITTER_BODY_SIZE = 140;

    @JsonView(View.Private.class)
    public String token;

    @JsonView(View.Private.class)
    public String secret;

    @JsonView(View.Private.class)
    public String username;

    public static boolean isWrongBody(String body) {
       return body == null || body.isEmpty() || body.length() > MAX_TWITTER_BODY_SIZE;
    }

    @Override
    public int getPrice() {
        return 0;
    }
}
