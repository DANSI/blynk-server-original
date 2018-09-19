package cc.blynk.server.core.model.widgets.outputs.graph;

import cc.blynk.server.core.model.DataStream;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.06.17.
 */
public class GraphDataStream {

    private final String title;

    private final GraphType graphType;

    private final int color;

    public final int targetId;

    @JsonProperty("pin") //todo "pin" for back compatibility
    public final DataStream dataStream;

    public final AggregationFunctionType functionType;

    private final int flip;

    private final String low;

    private final String high;

    private final String mathFormula;

    private final float yAxisMin;

    private final float yAxisMax;

    private final boolean showYAxis;

    private final String suffix;

    private final boolean cubicSmoothingEnabled;

    private final boolean connectMissingPointsEnabled;

    private final boolean isPercentMaxMin;

    private final YAxisScale yAxisScale;

    private final float delta;

    private final boolean userDeltaModifyAllowed;

    private final int maximumFractionDigits;

    @JsonCreator
    public GraphDataStream(@JsonProperty("title") String title,
                           @JsonProperty("graphType") GraphType graphType,
                           @JsonProperty("color") int color,
                           @JsonProperty("targetId") int targetId,
                           @JsonProperty("pin") DataStream dataStream,
                           @JsonProperty("functionType") AggregationFunctionType functionType,
                           @JsonProperty("flip") int flip,
                           @JsonProperty("low") String low,
                           @JsonProperty("high") String high,
                           @JsonProperty("mathFormula") String mathFormula,
                           @JsonProperty("yAxisMin") float yAxisMin,
                           @JsonProperty("yAxisMax") float yAxisMax,
                           @JsonProperty("showYAxis") boolean showYAxis,
                           @JsonProperty("suffix") String suffix,
                           @JsonProperty("cubicSmoothingEnabled") boolean cubicSmoothingEnabled,
                           @JsonProperty("connectMissingPointsEnabled") boolean connectMissingPointsEnabled,
                           @JsonProperty("isPercentMaxMin") boolean isPercentMaxMin,
                           @JsonProperty("yAxisScale") YAxisScale yAxisScale,
                           @JsonProperty("delta") float delta,
                           @JsonProperty("userDeltaModifyAllowed") boolean userDeltaModifyAllowed,
                           @JsonProperty("maximumFractionDigits") int maximumFractionDigits) {
        this.title = title;
        this.graphType = graphType;
        this.color = color;
        this.targetId = targetId;
        this.dataStream = dataStream;
        this.functionType = functionType;
        this.flip = flip;
        this.low = low;
        this.high = high;
        this.mathFormula = mathFormula;
        this.yAxisMin = yAxisMin;
        this.yAxisMax = yAxisMax;
        this.showYAxis = showYAxis;
        this.suffix = suffix;
        this.cubicSmoothingEnabled = cubicSmoothingEnabled;
        this.connectMissingPointsEnabled = connectMissingPointsEnabled;
        this.isPercentMaxMin = isPercentMaxMin;
        this.yAxisScale = yAxisScale == null ? YAxisScale.UNSET : yAxisScale;
        this.delta = delta;
        this.userDeltaModifyAllowed = userDeltaModifyAllowed;
        this.maximumFractionDigits = maximumFractionDigits;
    }

    public int getTargetId(int targetIdOverride) {
        return targetIdOverride == -1 ? this.targetId : targetIdOverride;
    }
}
