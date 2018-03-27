package cc.blynk.server;

import cc.blynk.utils.FileLoaderUtil;
import cc.blynk.utils.properties.GCMProperties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27.03.18.
 */
public class TextHolder {

    public volatile String tokenBody;
    public final String dynamicMailBody;
    public final String staticMailBody;
    public final String templateIdMailBody;
    public final String pushNotificationBody;

    public TextHolder(GCMProperties gcmProperties) {
        this.tokenBody = FileLoaderUtil.readTokenMailBody();
        this.dynamicMailBody = FileLoaderUtil.readDynamicMailBody();
        this.staticMailBody = FileLoaderUtil.readStaticMailBody();
        this.templateIdMailBody = FileLoaderUtil.readTemplateIdMailBody();
        this.pushNotificationBody = gcmProperties.getNotificationBody();
    }
}
