package cc.blynk.server.core.model.widgets.outputs.graph;

import cc.blynk.server.core.model.Pin;
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

    public final Pin pin;

    public final AggregationFunctionType functionType;

    public int flip;

    public String low;

    public String high;

    private final String mathFormula;

    private final int yAxisMin;

    private final int yAxisMax;

    private final String suffix;

    private final boolean cubicSmoothingEnabled;

    private final boolean connectMissingPointsEnabled;

    private final boolean isPercentMaxMin;

    @JsonCreator
    public GraphDataStream(@JsonProperty("title") String title,
                           @JsonProperty("graphType") GraphType graphType,
                           @JsonProperty("color") int color,
                           @JsonProperty("targetId") int targetId,
                           @JsonProperty("pin") Pin pin,
                           @JsonProperty("functionType") AggregationFunctionType functionType,
                           @JsonProperty("flip") int flip,
                           @JsonProperty("low") String low,
                           @JsonProperty("high") String high,
                           @JsonProperty("mathFormula") String mathFormula,
                           @JsonProperty("yAxisMin") int yAxisMin,
                           @JsonProperty("yAxisMax") int yAxisMax,
                           @JsonProperty("suffix") String suffix,
                           @JsonProperty("cubicSmoothingEnabled") boolean cubicSmoothingEnabled,
                           @JsonProperty("connectMissingPointsEnabled") boolean connectMissingPointsEnabled,
                           @JsonProperty("isPercentMaxMin") boolean isPercentMaxMin) {
        this.title = title;
        this.graphType = graphType;
        this.color = color;
        this.targetId = targetId;
        this.pin = pin;
        this.functionType = functionType;
        this.flip = flip;
        this.low = low;
        this.high = high;
        this.mathFormula = mathFormula;
        this.yAxisMin = yAxisMin;
        this.yAxisMax = yAxisMax;
        this.suffix = suffix;
        this.cubicSmoothingEnabled = cubicSmoothingEnabled;
        this.connectMissingPointsEnabled = connectMissingPointsEnabled;
        this.isPercentMaxMin = isPercentMaxMin;
    }
}
