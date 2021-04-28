package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.utils.structure.MapLimitedQueue;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.prependDashIdAndDeviceId;


/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Map extends OnePinWidget {

    private transient final MapLimitedQueue<String> lastCommands = new MapLimitedQueue<>();

    public boolean isPinToLatestPoint;

    public boolean isMyLocationSupported;

    public boolean isSatelliteMode;

    public String labelFormat;

    public int radius; //zoom level / radius which user selected.

    public float lat; // last user position on map

    public float lon; // last user position on map

    @Override
    public boolean updateIfSame(int deviceId, short pin, PinType type, String value) {
        if (isSame(deviceId, pin, type)) {
            switch (value) {
                case "clr" :
                    this.value = null;
                    this.lastCommands.clear();
                    break;
                default:
                    this.value = value;
                    this.lastCommands.add(value);
                    break;
            }
            return true;
        }
        return false;
    }

    @Override
    public void sendAppSync(Channel appChannel, int dashId, int targetId) {
        if (isNotValid() || lastCommands.size() == 0) {
            return;
        }
        if (targetId == ANY_TARGET || this.deviceId == targetId) {
            for (String storedValue : lastCommands) {
                String body = prependDashIdAndDeviceId(dashId, deviceId, makeHardwareBody(pinType, pin, storedValue));
                appChannel.write(makeUTF8StringMessage(APP_SYNC, SYNC_DEFAULT_MESSAGE_ID, body));
            }
        }
    }

    @Override
    public void sendHardSync(ChannelHandlerContext ctx, int msgId, int deviceId) {
    }

    @Override
    public String getJsonValue() {
        return JsonParser.toJson(lastCommands);
    }

    @Override
    public PinMode getModeType() {
        return PinMode.in;
    }

    @Override
    public int getPrice() {
        return 600;
    }

}
