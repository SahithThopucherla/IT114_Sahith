// st944 – 12/08/2025
// FINAL GameRoom for Rock–Paper–Scissors (Milestone 3)
// Handles READY, PICKS, winner calculation, scoreboard updates, and full
// integration with BaseGameRoom and ServerThread.

package Project.Server;

import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Exceptions.NotReadyException;
import Project.Exceptions.PlayerNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class GameRoom extends BaseGameRoom {

    // Stores picks for the round
    private final Map<Long, String> picks = new HashMap<>();

    public GameRoom(String name) {
        super(name);
    }

    // -----------------------------------------------------
    // SESSION + ROUND LIFECYCLE
    // -----------------------------------------------------
    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("SESSION START for room: " + name);

        changePhase(Phase.valueOf("STARTED"));

        // Reset states
        picks.clear();
        resetTurnStatus();
        resetReadyStatus();

        onRoundStart();
    }

    @Override
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("ROUND START for room: " + name);

        changePhase(Phase.valueOf("ROUND"));

        // Inform players
        broadcast("A new Rock–Paper–Scissors round has started!\nChoose Rock, Paper, or Scissors.");

        onTurnStart();
    }

    @Override
    protected void onTurnStart() {
        LoggerUtil.INSTANCE.info("TURN START for room: " + name);

        changePhase(Phase.valueOf("TURN"));
        broadcast("Waiting for all players to PICK...");
    }

    @Override
    protected void onTurnEnd() {
        LoggerUtil.INSTANCE.info("TURN END — calculating results");

        calculateWinner();
    }

    @Override
    protected void onRoundEnd() {
        LoggerUtil.INSTANCE.info("ROUND END — resetting picks");

        picks.clear();
        broadcast("Round ended.");

        // Automatically start next round after short delay
        onRoundStart();
    }

    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("SESSION END");

        broadcast("Not enough players ready — session cancelled.");
        changePhase(Phase.valueOf("READY"));

        resetReadyStatus();
        picks.clear();
    }

    // -----------------------------------------------------
    // PLAYER JOIN / LEAVE
    // -----------------------------------------------------
    @Override
    protected void onClientAdded(ServerThread client) {
        broadcast(client.getDisplayName() + " joined the room.");

        // Send phase sync to new client
        syncCurrentPhase(client);

        // Reset their local ready / turn state
        client.setReady(false);
        client.setTookTurn(false);

        // Sync who is ready
        syncReadyStatus(client);
    }

    @Override
    protected void onClientRemoved(ServerThread client) {
        if (client != null) {
            broadcast(client.getDisplayName() + " left the room.");
            picks.remove(client.getClientId());
        }

        // If fewer players remain than required -> end session
        if (clientsInRoom.size() < MINIMUM_REQUIRED_TO_START
            && currentPhase != Phase.valueOf("READY")) {
            onSessionEnd();
        }
    }

    // -----------------------------------------------------
    // READY BUTTON HANDLER
    // -----------------------------------------------------
    @Override
    protected void handleReady(ServerThread sender) {
        try {
            checkPlayerInRoom(sender);
            checkCurrentPhase(sender, Phase.valueOf("READY"));

            ServerThread p = clientsInRoom.get(sender.getClientId());
            p.setReady(true);

            sendReadyStatus(p, true);
            broadcast(sender.getDisplayName() + " is READY");

            // Auto-start handled by BaseGameRoom timer
            startReadyTimer(false);

        } catch (Exception e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Cannot READY: " + e.getMessage());
        }
    }

    // -----------------------------------------------------
    // PLAYER PICK HANDLER
    // -----------------------------------------------------
    public void handlePick(ServerThread sender, String choice) {
        try {
            checkPlayerInRoom(sender);
            checkIsReady(sender); // must have clicked READY
            checkCurrentPhase(sender, Phase.valueOf("TURN"));

            choice = choice.toLowerCase();

            if (!choice.equals("r") && !choice.equals("p") && !choice.equals("s")) {
                sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid pick.");
                return;
            }

            picks.put(sender.getClientId(), choice);
            sender.setTookTurn(true);

            broadcast(sender.getDisplayName() + " has made their pick.");

            // If everyone picked → end turn
            if (allPlayersPicked()) {
                onTurnEnd();
            }

        } catch (NotReadyException nre) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "You are not READY.");
        } catch (PlayerNotFoundException pnfe) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "You are not in this room.");
        } catch (Exception e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Error handling pick: " + e.getMessage());
        }
    }

    private boolean allPlayersPicked() {
        return picks.size() == clientsInRoom.size();
    }

    // -----------------------------------------------------
    // WINNER CALCULATION
    // -----------------------------------------------------
    private void calculateWinner() {
        if (picks.size() < 2) {
            broadcast("Not enough players picked. Round cancelled.");
            onRoundEnd();
            return;
        }

        Map<Long, String> playerPicks = new HashMap<>(picks);

        // Build scoreboard text
        StringBuilder sb = new StringBuilder("\n=== ROUND RESULTS ===\n");

        playerPicks.forEach((id, pick) -> {
            String playerName = clientsInRoom.get(id).getDisplayName();
            sb.append(playerName).append(" → ").append(fullPickName(pick)).append("\n");
        });

        // Determine winner(s)
        String result = determineWinner(playerPicks);
        sb.append("\n").append(result);

        broadcast(sb.toString());

        onRoundEnd();
    }

    private String determineWinner(Map<Long, String> picks) {
        // Count picks
        long rCount = picks.values().stream().filter(ch -> ch.equals("r")).count();
        long pCount = picks.values().stream().filter(ch -> ch.equals("p")).count();
        long sCount = picks.values().stream().filter(ch -> ch.equals("s")).count();

        // Tie conditions (all three appear or only one)
        if ((rCount > 0 && pCount > 0 && sCount > 0) ||
            (rCount == picks.size() || pCount == picks.size() || sCount == picks.size())) {
            return "Result: DRAW — everyone cancels each other out!";
        }

        // Determine winning shape
        String winning = null;
        if (rCount > 0 && sCount > 0 && pCount == 0) winning = "r";
        if (pCount > 0 && rCount > 0 && sCount == 0) winning = "p";
        if (sCount > 0 && pCount > 0 && rCount == 0) winning = "s";

        if (winning == null) return "Result: DRAW — no decisive winner.";

        StringBuilder winners = new StringBuilder("Winner(s):\n");
        for (var entry : picks.entrySet()) {
            if (entry.getValue().equals(winning)) {
                winners.append(" • ").append(clientsInRoom.get(entry.getKey()).getDisplayName()).append("\n");
            }
        }
        return winners.toString();
    }

    private String fullPickName(String p) {
        return switch (p) {
            case "r" -> "ROCK";
            case "p" -> "PAPER";
            case "s" -> "SCISSORS";
            default -> p;
        };
    }

    // -----------------------------------------------------
    // HELPERS
    // -----------------------------------------------------
    // Reset tookTurn flag for all players
    private void resetTurnStatus() {
        clientsInRoom.values().forEach(c -> c.setTookTurn(false));
    }

    @Override
    protected void broadcast(String msg) {
        clientsInRoom.values().forEach(sp -> sp.sendMessage(Constants.DEFAULT_CLIENT_ID, msg));
    }
}
