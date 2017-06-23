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

    private final GraphType graphType;

    private final int color;

    private final int targetId;

    private final Pin pin;

    private final String mathFormula;

    private final int yAxisMin;

    private final int yAxisMax;

    private final String suffix;

    private final boolean cubicSmoothingEnabled;

    private final boolean connectMissingPointsEnabled;

    @JsonCreator
    public GraphDataStream(@JsonProperty("graphType") GraphType graphType,
                           @JsonProperty("color") int color,
                           @JsonProperty("targetId") int targetId,
                           @JsonProperty("targetId") Pin pin,
                           @JsonProperty("mathFormula") String mathFormula,
                           @JsonProperty("yAxisMin") int yAxisMin,
                           @JsonProperty("yAxisMax") int yAxisMax,
                           @JsonProperty("suffix") String suffix,
                           @JsonProperty("cubicSmoothingEnabled") boolean cubicSmoothingEnabled,
                           @JsonProperty("connectMissingPointsEnabled") boolean connectMissingPointsEnabled) {
        this.graphType = graphType;
        this.color = color;
        this.targetId = targetId;
        this.pin = pin;
        this.mathFormula = mathFormula;
        this.yAxisMin = yAxisMin;
        this.yAxisMax = yAxisMax;
        this.suffix = suffix;
        this.cubicSmoothingEnabled = cubicSmoothingEnabled;
        this.connectMissingPointsEnabled = connectMissingPointsEnabled;
    }
}
