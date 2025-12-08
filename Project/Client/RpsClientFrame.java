// st944 - 12-07-2025
// Main Swing window for the Rock Paper Scissors client UI.
// Hosts the connection panel and the game panel and wires them to the Client backend.

package Project.Client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class RpsClientFrame extends JFrame implements ClientUi {

    private final Client client;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final ConnectionPanel connectionPanel;
    private final GamePanel gamePanel;

    public RpsClientFrame(Client client) {
        super("Rock Paper Scissors - st944");
        this.client = client;

        // Connect backend to this UI
        this.client.setUi(this);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        connectionPanel = new ConnectionPanel(client, this);
        gamePanel = new GamePanel(client);

        cardPanel.add(connectionPanel, "connection");
        cardPanel.add(gamePanel, "game");

        setLayout(new BorderLayout());
        add(cardPanel, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    // Show the connection UI panel
    public void showConnection() {
        SwingUtilities.invokeLater(() -> cardLayout.show(cardPanel, "connection"));
    }

    // Show the main game UI panel
    public void showGame() {
        SwingUtilities.invokeLater(() -> cardLayout.show(cardPanel, "game"));
    }

    // ========================================================================
    //             ClientUi CALLBACKS (client backend → UI)
    // ========================================================================

    @Override
    public void onConnected(long clientId) {
        gamePanel.setClientId(clientId);
        showGame();
    }

    @Override
    public void onLogMessage(String message) {
        if (gamePanel != null) {
            gamePanel.appendLog(message);
        }
    }

    @Override
    public void onRoomChanged(String roomName) {
        if (gamePanel != null) {
            gamePanel.setRoomName(roomName);
        }
    }

    @Override
    public void onPhaseChanged(String phaseText) {
        if (gamePanel != null) {
            gamePanel.setPhase(phaseText);
        }
    }

    @Override
    public void onScoreboardUpdated(String scoreboard) {
        if (gamePanel != null) {
            gamePanel.setScoreboard(scoreboard);
        }
    }

    @Override
    public void onGameMessage(String message) {
        if (gamePanel != null) {
            gamePanel.appendLog(message);
        }
    }

    @Override
    public void onTurnStatusChanged(long clientId, boolean isMyTurn) {
        if (gamePanel != null) {
            gamePanel.appendLog(String.format("Turn status changed: client %d is %s the turn", clientId, isMyTurn ? "on" : "not"));
        }
    }

    @Override
    public void onReadyStatusChanged(long clientId, boolean isReady) {
        if (gamePanel != null) {
            gamePanel.appendLog(String.format("Ready status changed: client %d is %s", clientId, isReady ? "ready" : "not ready"));
        }
    }
}
