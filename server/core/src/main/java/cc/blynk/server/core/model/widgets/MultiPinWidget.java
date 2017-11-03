package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.StringJoiner;

import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.11.15.
 */
public abstract class MultiPinWidget extends Widget implements AppSyncWidget {

    public int deviceId;

    @JsonProperty("pins") //todo "pins" for back compatibility
    public DataStream[] dataStreams;

    @Override
    public boolean updateIfSame(int deviceId, byte pinIn, PinType type, String value) {
        boolean isSame = false;
        if (this.dataStreams != null && this.deviceId == deviceId) {
            for (DataStream dataStream : this.dataStreams) {
                if (dataStream.isSame(pinIn, type)) {
                    dataStream.value = value;
                    isSame = true;
                }
            }
        }
        return isSame;
    }

    @Override
    public void updateIfSame(Widget widget) {
        if (widget instanceof MultiPinWidget) {
            MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
            if (multiPinWidget.dataStreams != null && multiPinWidget.deviceId == this.deviceId) {
                for (DataStream dataStream : multiPinWidget.dataStreams) {
                    updateIfSame(multiPinWidget.deviceId, dataStream.pin, dataStream.pinType, dataStream.value);
                }
            }
        }
    }

    @Override
    public boolean isSame(int deviceId, byte pinIn, PinType pinType) {
        if (dataStreams != null && this.deviceId == deviceId) {
            for (DataStream dataStream : dataStreams) {
                if (dataStream.isSame(pinIn, pinType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public abstract boolean isSplitMode();

    public String makeHardwareBody(byte pinIn, PinType pinType) {
        if (dataStreams == null) {
            return null;
        }
        if (isSplitMode()) {
            for (DataStream dataStream : dataStreams) {
                if (dataStream.isSame(pinIn, pinType)) {
                    return dataStream.makeHardwareBody();
                }
            }
        } else {
            if (dataStreams[0].notEmpty()) {
                StringBuilder sb = new StringBuilder(dataStreams[0].makeHardwareBody());
                for (int i = 1; i < dataStreams.length; i++) {
                    if (dataStreams[i].notEmpty()) {
                        sb.append(BODY_SEPARATOR).append(dataStreams[i].value);
                    }
                }
                return sb.toString();
            }
        }
        return null;
    }

    @Override
    public void append(StringBuilder sb, int deviceId) {
        if (dataStreams != null && this.deviceId == deviceId) {
            for (DataStream dataStream : dataStreams) {
                append(sb, dataStream.pin, dataStream.pinType, getModeType());
            }
        }
    }

    @Override
    public String getJsonValue() {
        if (dataStreams == null) {
            return "[]";
        }
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (DataStream dataStream : dataStreams) {
            if (dataStream.value == null) {
                sj.add("\"\"");
            } else {
                sj.add("\"" + dataStream.value + "\"");
            }
        }
        return sj.toString();
    }

}
