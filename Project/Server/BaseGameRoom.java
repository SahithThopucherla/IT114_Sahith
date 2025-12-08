// st944 – Updated 12/07/2025
// Core logic for GameRoom lifecycle (ready → session → rounds → end)

package Project.Server;

import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.TimedEvent;
import Project.Exceptions.NotReadyException;
import Project.Exceptions.PhaseMismatchException;
import Project.Exceptions.PlayerNotFoundException;

public abstract class BaseGameRoom extends Room {

    // Ready timer (before session begins)
    private TimedEvent readyTimer = null;

    // Minimum players needed to start
    protected final int MINIMUM_REQUIRED_TO_START = 2;

    // Current game lifecycle phase
    protected Phase currentPhase = Phase.READY;

    // Allow toggle (Milestone 3 disables toggling)
    protected boolean allowToggleReady = false;

    public BaseGameRoom(String name) {
        super(name);
    }

    // ===== ABSTRACT LIFECYCLE CALLBACKS =====

    protected abstract void onSessionStart();
    protected abstract void onRoundStart();
    protected abstract void onTurnStart();
    protected abstract void onTurnEnd();
    protected abstract void onRoundEnd();
    protected abstract void onSessionEnd();
    @Override
    protected abstract void onClientAdded(ServerThread client);
    @Override
    protected abstract void onClientRemoved(ServerThread client);

    // ===== ROOM OVERRIDES =====

    @Override
    public synchronized void addClient(ServerThread client) {
        super.addClient(client);
        onClientAdded(client);
    }

    @Override
    public synchronized void removeClient(ServerThread client) {
        super.removeClient(client);
        onClientRemoved(client);
    }

    // Not overriding a Room.disconnect(ServerThread) (Room has no such method),
    // delegate to Room.removeClient to perform the disconnect cleanup.
    public synchronized void disconnect(ServerThread client) {
        super.removeClient(client);
        onClientRemoved(client);
        onClientRemoved(client);
    }

    // ===== READY TIMER =====

    protected void resetReadyTimer() {
        if (readyTimer != null) {
            readyTimer.cancel();
            readyTimer = null;
        }
    }

    protected void startReadyTimer(boolean resetFirst) {
        if (resetFirst) resetReadyTimer();

        if (readyTimer == null) {
            readyTimer = new TimedEvent(30, this::checkReadyStatus);
            readyTimer.setTickCallback(time ->
                    System.out.println("Ready Timer: " + time));
        }
    }

    private void checkReadyStatus() {
        long numReady = clientsInRoom.values().stream()
                .filter(ServerThread::isReady)
                .count();

        if (numReady >= MINIMUM_REQUIRED_TO_START) {
            resetReadyTimer();
            onSessionStart();
        } else {
            onSessionEnd();
        }
    }

    protected void resetReadyStatus() {
        clientsInRoom.values().forEach(p -> p.setReady(false));
        sendResetReadyTrigger();
    }

    // ===== PHASE SYNC =====

    protected void changePhase(Phase phase) {
        if (currentPhase != phase) {
            currentPhase = phase;
            sendCurrentPhase();
        }
    }

    protected void syncCurrentPhase(ServerThread sp) {
        sp.sendCurrentPhase(currentPhase);
    }

    protected void sendCurrentPhase() {
        clientsInRoom.values().removeIf(sp -> {
            boolean fail = !sp.sendCurrentPhase(currentPhase);
            if (fail) removeClient(sp);
            return fail;
        });
    }

    // ===== READY STATUS SYNC =====

    protected void sendResetReadyTrigger() {
        clientsInRoom.values().removeIf(sp -> {
            boolean fail = !sp.sendResetReady();
            if (fail) removeClient(sp);
            return fail;
        });
    }

    protected void syncReadyStatus(ServerThread incoming) {
        clientsInRoom.values().removeIf(sp -> {
            boolean fail = !incoming.sendReadyStatus(
                    sp.getClientId(), sp.isReady(), true
            );
            if (fail) removeClient(sp);
            return fail;
        });
    }

    protected void sendReadyStatus(ServerThread who, boolean isReady) {
        clientsInRoom.values().removeIf(sp -> {
            boolean fail = !sp.sendReadyStatus(who.getClientId(), isReady, true);
            if (fail) removeClient(sp);
            return fail;
        });
    }

    // ===== HANDLE READY FROM CLIENT =====

    protected void handleReady(ServerThread sender) {
        try {
            checkPlayerInRoom(sender);
            checkCurrentPhase(sender, Phase.READY);

            ServerThread st = clientsInRoom.get(sender.getClientId());

            if (!allowToggleReady) {
                st.setReady(true);
            } else {
                st.setReady(!st.isReady());
            }

            startReadyTimer(false);
            sendReadyStatus(st, st.isReady());

        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("handleReady() error:", e);
        }
    }

    // ===== LOGIC CHECKS =====

    protected void checkCurrentPhase(ServerThread client, Phase required)
            throws Exception {

        if (currentPhase != required) {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Current phase is " + currentPhase + ". Try again later.");
            throw new PhaseMismatchException("Invalid phase");
        }
    }

    protected void checkIsReady(ServerThread client)
            throws NotReadyException {

        if (!client.isReady()) {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You must be READY first.");
            throw new NotReadyException("Not ready");
        }
    }

    protected void checkPlayerInRoom(ServerThread client)
            throws Exception {

        if (!clientsInRoom.containsKey(client.getClientId())) {
            LoggerUtil.INSTANCE.severe("Player not in room");
            throw new PlayerNotFoundException("Player missing");
        }
    }
}
