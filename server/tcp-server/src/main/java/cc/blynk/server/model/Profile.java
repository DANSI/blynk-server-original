package cc.blynk.server.model;

import cc.blynk.common.model.messages.Message;
import cc.blynk.server.dao.graph.GraphKey;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.model.widgets.Widget;
import cc.blynk.server.model.widgets.others.Timer;
import cc.blynk.server.model.widgets.outputs.Graph;
import cc.blynk.server.utils.JsonParser;

import java.util.*;


/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 13:04
 */
public class Profile {

    private final transient Set<GraphKey> graphPins = new HashSet<>();
    //todo avoid volatile
    public volatile Integer activeDashId;
    /**
     * Specific property used for improving user experience on mobile application.
     * In case user activated dashboard before hardware connected to server, user have to
     * deactivate and activate dashboard again in order to setup PIN MODES (OUT, IN).
     * With this property problem resolved by server side. Command for setting Pin Modes
     * is remembered and when hardware goes online - server sends Pin Modes command to hardware
     * without requiring user to activate/deactivate dashboard again.
     */
    //todo avoid volatile
    public volatile transient Message pinModeMessage;
    private DashBoard[] dashBoards;

    //defines which server user users Blynk or own local server.
    private Boolean isLocal;
    private String connectionType;
    
    /**
     * Check if dashboardId is real and exists in user profile.
     */
    public void validateDashId(int dashBoardId, int msgId) {
        if (dashBoards != null) {
            for (DashBoard dashBoard : dashBoards) {
                if (dashBoard.getId() == dashBoardId) {
                    return;
                }
            }
        }

        throw new IllegalCommandException(String.format("Requested token for non-existing '%d' dash id.", dashBoardId), msgId);
    }

    public DashBoard[] getDashBoards() {
        return dashBoards;
    }

    public void setDashBoards(DashBoard[] dashBoards) {
        this.dashBoards = dashBoards;
    }

    public List<Timer> getActiveDashboardTimerWidgets() {
        if (dashBoards == null || dashBoards.length == 0 || activeDashId == null) {
            return Collections.emptyList();
        }

        DashBoard dashBoard = getActiveDashBoard();
        if (dashBoard == null) {
            return Collections.emptyList();
        }

        return dashBoard.getTimerWidgets();
    }

    public <T> T getActiveDashboardWidgetByType(Class<T> clazz) {
        if (dashBoards == null || dashBoards.length == 0 || activeDashId == null) {
            return null;
        }

        DashBoard dashBoard = getActiveDashBoard();
        if (dashBoard == null) {
            return null;
        }

        return dashBoard.getWidgetByType(clazz);
    }

    public DashBoard getActiveDashBoard() {
        for (DashBoard dashBoard : dashBoards) {
            if (dashBoard.getId() == activeDashId) {
                return dashBoard;
            }
        }
        return null;
    }

    public void calcGraphPins() {
        if (dashBoards == null || dashBoards.length == 0) {
            return;
        }

        for (DashBoard dashBoard : dashBoards) {
            if (dashBoard.getWidgets() == null || dashBoard.getWidgets().length == 0) {
                continue;
            }

            for (Widget widget : dashBoard.getWidgets()) {
                if (widget instanceof Graph) {
                    if (widget.pin != null) {
                        graphPins.add(new GraphKey(dashBoard.getId(), widget.pin, widget.pinType));
                    }
                }
            }
        }
    }

    public boolean hasGraphPin(GraphKey key) {
        return graphPins != null && key != null && graphPins.contains(key);
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Profile that = (Profile) o;

        if (!Arrays.equals(dashBoards, that.dashBoards)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return dashBoards != null ? Arrays.hashCode(dashBoards) : 0;
    }
}
