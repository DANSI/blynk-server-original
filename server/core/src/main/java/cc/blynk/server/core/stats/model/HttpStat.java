package cc.blynk.server.core.stats.model;

import static cc.blynk.server.core.protocol.enums.Command.HTTP_EMAIL;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_GET_PIN_DATA;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_GET_PROJECT;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_IS_APP_CONNECTED;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_IS_HARDWARE_CONNECTED;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_NOTIFY;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_TOTAL;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_UPDATE_PIN_DATA;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 11.01.17.
 */
public class HttpStat {

    public int isHardwareConnectedCount;
    public int isAppConnectedCount;
    public int getPinDataCount;
    public int updatePinDataCount;
    public int notifyCount;
    public int emailCount;
    public int getProjectCount;
    public int totalCount;

    public void assign(short field, int val) {
        switch (field) {
            case HTTP_IS_HARDWARE_CONNECTED :
                this.isHardwareConnectedCount = val;
                break;
            case HTTP_IS_APP_CONNECTED :
                this.isAppConnectedCount = val;
                break;
            case HTTP_GET_PIN_DATA :
                this.getPinDataCount = val;
                break;
            case HTTP_UPDATE_PIN_DATA :
                this.updatePinDataCount = val;
                break;
            case HTTP_NOTIFY :
                this.notifyCount = val;
                break;
            case HTTP_EMAIL :
                this.emailCount = val;
                break;
            case HTTP_GET_PROJECT :
                this.getProjectCount = val;
                break;
            case HTTP_TOTAL :
                this.totalCount = val;
                break;
        }
    }
}

