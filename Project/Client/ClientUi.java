package Project.Client;

public interface ClientUi {
    void onConnected(long clientId);
    void onLogMessage(String message);
    void onRoomChanged(String roomName);
    void onPhaseChanged(String phase);

    // NEW
    void onReadyStatusChanged(long clientId, boolean isReady);
    void onTurnStatusChanged(long clientId, boolean tookTurn);
    void onScoreboardUpdated(String scoreboard);
    void onGameMessage(String text);
}
