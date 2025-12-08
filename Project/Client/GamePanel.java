// st944 - 12-07-2025
// Game UI panel for Rock Paper Scissors.
// Handles room creation, joining, ready state, and pick selection.

package Project.Client;

import java.awt.*;
import javax.swing.*;

public class GamePanel extends JPanel {

    private final Client client;

    private JLabel lblClientId;
    private JLabel lblRoom;
    private JLabel lblPhase;
    private JTextArea txtLog;

    private JTextField txtCreateRoom;
    private JButton btnCreateRoom;

    private JTextField txtJoinRoom;
    private JButton btnJoinRoom;

    private JButton btnReady;

    private JButton btnRock;
    private JButton btnPaper;
    private JButton btnScissors;

    public GamePanel(Client client) {
        this.client = client;

        setLayout(new BorderLayout());

        // =====================
        // TOP INFO PANEL
        // =====================
        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        lblClientId = new JLabel("Client ID: (not connected)");
        lblRoom = new JLabel("Room: lobby");
        lblPhase = new JLabel("Phase: READY");

        topPanel.add(lblClientId);
        topPanel.add(lblRoom);
        topPanel.add(lblPhase);

        add(topPanel, BorderLayout.NORTH);

        // =====================
        // CENTER LOG PANEL
        // =====================
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(txtLog);
        add(scrollPane, BorderLayout.CENTER);

        // =====================
        // BOTTOM CONTROLS
        // =====================
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridLayout(4, 1));

        // ---- ROOM CONTROLS ----
        JPanel roomPanel = new JPanel();
        txtCreateRoom = new JTextField(10);
        btnCreateRoom = new JButton("Create Room");

        txtJoinRoom = new JTextField(10);
        btnJoinRoom = new JButton("Join Room");

        roomPanel.add(new JLabel("Create:"));
        roomPanel.add(txtCreateRoom);
        roomPanel.add(btnCreateRoom);

        roomPanel.add(new JLabel("Join:"));
        roomPanel.add(txtJoinRoom);
        roomPanel.add(btnJoinRoom);

        bottomPanel.add(roomPanel);

        // ---- READY BUTTON ----
        btnReady = new JButton("Ready");
        JPanel readyPanel = new JPanel();
        readyPanel.add(btnReady);
        bottomPanel.add(readyPanel);

        // ---- PICK BUTTONS ----
        JPanel picksPanel = new JPanel();
        btnRock = new JButton("Rock");
        btnPaper = new JButton("Paper");
        btnScissors = new JButton("Scissors");

        picksPanel.add(btnRock);
        picksPanel.add(btnPaper);
        picksPanel.add(btnScissors);

        bottomPanel.add(picksPanel);

        add(bottomPanel, BorderLayout.SOUTH);

        // ======================================================
        // BUTTON ACTION LISTENERS — FIXED FOR MILESTONE 3
        // ======================================================

        // CREATE ROOM
        btnCreateRoom.addActionListener(e -> {
            String roomName = txtCreateRoom.getText().trim();
            if (!roomName.isEmpty()) {
                client.createRoomFromUi(roomName);     // REAL CLIENT METHOD
                appendLog("Attempting to create room: " + roomName);
            } else {
                appendLog("Enter a room name first.");
            }
        });

        // JOIN ROOM
        btnJoinRoom.addActionListener(e -> {
            String roomName = txtJoinRoom.getText().trim();
            if (!roomName.isEmpty()) {
                client.joinRoomFromUi(roomName);       // REAL CLIENT METHOD
                appendLog("Attempting to join room: " + roomName);
            } else {
                appendLog("Enter a room name first.");
            }
        });

        // READY BUTTON
        btnReady.addActionListener(e -> {
            client.sendReady();
            appendLog("You marked yourself READY.");
        });

        // PICK BUTTONS
        btnRock.addActionListener(e -> sendPick("r"));
        btnPaper.addActionListener(e -> sendPick("p"));
        btnScissors.addActionListener(e -> sendPick("s"));
    }

    // ======================================================
    // Helper for picks
    // ======================================================
    private void sendPick(String choice) {
        client.sendPick(choice);
        appendLog("You picked: " + choice.toUpperCase());
    }

    // ======================================================
    // PUBLIC UI UPDATE CALLBACK METHODS
    // ======================================================

    public void setClientId(long id) {
        lblClientId.setText("Client ID: " + id);
    }

    public void setRoomName(String roomName) {
        lblRoom.setText("Room: " + roomName);
    }

    public void setPhase(String phaseText) {
        lblPhase.setText("Phase: " + phaseText);
    }

    public void setScoreboard(String scoreboard) {
        appendLog("SCOREBOARD:\n" + scoreboard + "\n");
    }

    public void appendLog(String text) {
        txtLog.append(text + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }
}
