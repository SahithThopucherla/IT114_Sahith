package Project.Client;

import Project.Common.Command;
import Project.Common.ConnectionPayload;
import Project.Common.LoggerUtil;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Common.Phase;
import Project.Common.ReadyPayload;
import Project.Common.RoomAction;
import Project.Common.RoomResultPayload;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;
import Project.Common.User;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;

/**
 * Official RPS Client with integrated UI.
 * Keeps ALL original text-command features + adds full Swing UI.
 */
public enum Client {
    INSTANCE;

    // --------------------- LOGGER SETUP -----------------------
    {
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024);
        config.setFileCount(1);
        config.setLogLocation("client.log");
        LoggerUtil.INSTANCE.setConfig(config);
    }

    // ------------------ NETWORK STATE -------------------------
    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private volatile boolean isRunning = true;
    private final ConcurrentHashMap<Long, User> knownClients = new ConcurrentHashMap<>();
    private User myUser = new User();
    private Phase currentPhase = Phase.READY;

    // Regex for /connect
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");

    // ------------------ UI COMPONENTS --------------------------
    private JFrame uiFrame;
    private JTextArea uiLog;
    private JLabel uiClientIdLabel;
    private JLabel uiRoomLabel;
    private JLabel uiPhaseLabel;

    // -------------------------------------------------------------
    // Utility logging
    // -------------------------------------------------------------
    private void error(String message) {
        LoggerUtil.INSTANCE.severe(TextFX.colorize(message, Color.RED));
        uiLog("ERROR: " + message);
    }

    private Client() {
        LoggerUtil.INSTANCE.info("Client created");
    }

    // -------------------------------------------------------------
    // CONNECTION
    // -------------------------------------------------------------
    public boolean isConnected() {
        if (server == null) return false;
        return server.isConnected() && !server.isClosed() &&
                !server.isInputShutdown() && !server.isOutputShutdown();
    }

    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new ObjectOutputStream(server.getOutputStream());
            in = new ObjectInputStream(server.getInputStream());

            LoggerUtil.INSTANCE.info("Client connected");
            uiLog("Connected to server.");

            CompletableFuture.runAsync(this::listenToServer);
        } catch (Exception e) {
            error("Connection failed: " + e.getMessage());
        }
        return isConnected();
    }

    // UI wrapper for connect
    public boolean uiConnect(String host, int port, String username) {
        myUser.setClientName(username);
        boolean ok = connect(host, port);
        if (ok) {
            try { sendClientName(username); } catch (Exception ignored) {}
        }
        return ok;
    }

    // -------------------------------------------------------------
    // COMMAND PARSING (kept 100% original)
    // -------------------------------------------------------------
    private boolean isConnection(String text) {
        Matcher ip = ipAddressPattern.matcher(text);
        Matcher local = localhostPattern.matcher(text);
        return ip.matches() || local.matches();
    }

    private boolean processClientCommand(String text) throws IOException {
        boolean wasCommand = false;

        if (!text.startsWith("/")) return false;

        text = text.substring(1);

        if (isConnection("/" + text)) {
            if (myUser.getClientName() == null || myUser.getClientName().isEmpty()) {
                error("Use /name <yourname> first");
                return true;
            }
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0], Integer.parseInt(parts[1]));
            sendClientName(myUser.getClientName());
            return true;
        }

        if (text.startsWith("name")) {
            text = text.replace("name", "").trim();
            myUser.setClientName(text);
            uiLog("Name set to: " + text);
            wasCommand = true;
        }

        else if (text.startsWith(Command.CREATE_ROOM.command)) {
            text = text.replace(Command.CREATE_ROOM.command, "").trim();
            sendRoomAction(text, RoomAction.CREATE);
            wasCommand = true;
        }

        else if (text.startsWith(Command.JOIN_ROOM.command)) {
            text = text.replace(Command.JOIN_ROOM.command, "").trim();
            sendRoomAction(text, RoomAction.JOIN);
            wasCommand = true;
        }

        else if (text.startsWith(Command.LEAVE_ROOM.command)) {
            sendRoomAction("lobby", RoomAction.LEAVE);
            wasCommand = true;
        }

        else if (text.startsWith(Command.LIST_ROOMS.command)) {
            sendRoomAction("", RoomAction.LIST);
            wasCommand = true;
        }

        else if (text.startsWith("pick")) {
            String choice = text.replace("pick", "").trim().substring(0, 1).toLowerCase();
            sendPick(choice);
            wasCommand = true;
        }

        else if (text.equalsIgnoreCase("ready")) {
            sendReady();
            wasCommand = true;
        }

        else if (text.equalsIgnoreCase("quit")) {
            close();
            wasCommand = true;
        }

        return wasCommand;
    }

    // -------------------------------------------------------------
    // SEND METHODS
    // -------------------------------------------------------------
    private void sendClientName(String name) throws IOException {
        ConnectionPayload p = new ConnectionPayload();
        p.setClientName(name);
        p.setPayloadType(PayloadType.CLIENT_CONNECT);
        sendToServer(p);
    }

    private void sendPick(String choice) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.PICK);
        p.setMessage(choice);
        sendToServer(p);
    }

    private void sendReady() throws IOException {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(PayloadType.READY);
        sendToServer(rp);
    }

    private void sendRoomAction(String roomName, RoomAction action) throws IOException {
        Payload p = new Payload();
        p.setMessage(roomName);

        switch (action) {
            case CREATE: p.setPayloadType(PayloadType.ROOM_CREATE); break;
            case JOIN:   p.setPayloadType(PayloadType.ROOM_JOIN);   break;
            case LEAVE:  p.setPayloadType(PayloadType.ROOM_LEAVE);  break;
            case LIST:   p.setPayloadType(PayloadType.ROOM_LIST);   break;
        }
        sendToServer(p);
    }

    private void sendToServer(Payload payload) throws IOException {
        if (isConnected()) {
            out.writeObject(payload);
            out.flush();
        }
    }

    // -------------------------------------------------------------
    // RECEIVE LOOP
    // -------------------------------------------------------------
    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                Payload incoming = (Payload) in.readObject();
                if (incoming != null) processPayload(incoming);
            }
        } catch (Exception e) {
            uiLog("Server disconnected.");
        } finally {
            closeServerConnection();
        }
    }

    // -------------------------------------------------------------
    // PAYLOAD PROCESSING
    // -------------------------------------------------------------
    private void processPayload(Payload payload) {
        switch (payload.getPayloadType()) {

            case CLIENT_ID:
                processClientId(payload);
                break;

            case ROOM_JOIN:
            case ROOM_LEAVE:
            case SYNC_CLIENT:
                processRoomAction(payload);
                break;

            case PHASE:
                currentPhase = Phase.valueOf(payload.getMessage());
                uiSetPhase(payload.getMessage());
                break;

            case READY:
            case SYNC_READY:
                processReady(payload);
                break;

            case MESSAGE:
                uiLog(payload.getMessage());
                break;

            case PICK:
                uiLog("Pick acknowledged.");
                break;

            case ROOM_LIST:
                processList(payload);
                break;

            default:
                uiLog("Unhandled payload: " + payload.getPayloadType());
        }
    }

    private void processClientId(Payload payload) {
        myUser.setClientId(payload.getClientId());
        myUser.setClientName(((ConnectionPayload)payload).getClientName());
        knownClients.put(myUser.getClientId(), myUser);
        uiSetClientId(payload.getClientId());
        uiLog("Assigned client ID: " + payload.getClientId());
    }

    private void processList(Payload payload) {
        RoomResultPayload r = (RoomResultPayload) payload;
        uiLog("Rooms:\n" + String.join("\n", r.getRooms()));
    }

    private void processReady(Payload payload) {
        ReadyPayload rp = (ReadyPayload) payload;
        uiLog("Ready status: client " + rp.getClientId() + " = " + rp.isReady());
    }

    private void processRoomAction(Payload payload) {
        ConnectionPayload cp = (ConnectionPayload) payload;

        if (cp.getPayloadType() == PayloadType.ROOM_JOIN) {
            uiLog("Joined room: " + cp.getMessage());
            uiSetRoom(cp.getMessage());
        }

        if (cp.getPayloadType() == PayloadType.ROOM_LEAVE) {
            uiLog("Left room.");
            uiSetRoom("lobby");
        }
    }

    // -------------------------------------------------------------
    // LOCAL INPUT LISTENER (kept exactly as original)
    // -------------------------------------------------------------
    public void start() throws IOException {
        CompletableFuture.runAsync(this::listenToInput).join();
    }

    private void listenToInput() {
        try (Scanner sc = new Scanner(System.in)) {
            while (isRunning) {
                String line = sc.nextLine();
                if (!processClientCommand(line)) {
                    sendMessage(line);
                }
            }
        } catch (Exception ignored) {}
    }

    private void sendMessage(String msg) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setMessage(msg);
        sendToServer(p);
    }

    // -------------------------------------------------------------
    // CLEANUP
    // -------------------------------------------------------------
    private void close() {
        isRunning = false;
        closeServerConnection();
        System.exit(0);
    }

    private void closeServerConnection() {
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (server != null) server.close(); } catch (Exception ignored) {}
    }

    // -------------------------------------------------------------
    // UI HELPER METHODS
    // -------------------------------------------------------------
    private void uiLog(String text) {
        if (uiLog != null) {
            SwingUtilities.invokeLater(() -> {
                uiLog.append(text + "\n");
                uiLog.setCaretPosition(uiLog.getDocument().getLength());
            });
        }
    }

    private void uiSetClientId(long id) {
        SwingUtilities.invokeLater(() -> uiClientIdLabel.setText("Client ID: " + id));
    }

    private void uiSetRoom(String name) {
        SwingUtilities.invokeLater(() -> uiRoomLabel.setText("Room: " + name));
    }

    private void uiSetPhase(String phase) {
        SwingUtilities.invokeLater(() -> uiPhaseLabel.setText("Phase: " + phase));
    }

    // -------------------------------------------------------------
    // BUILD FULL UI
    // -------------------------------------------------------------
    private void buildUI() {
        uiFrame = new JFrame("RPS Client");
        uiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        uiFrame.setSize(800, 600);

        JPanel root = new JPanel(new BorderLayout());
        uiFrame.setContentPane(root);

        // Top Status Bar
        JPanel status = new JPanel(new GridLayout(3, 1));
        uiClientIdLabel = new JLabel("Client ID: ?");
        uiRoomLabel = new JLabel("Room: lobby");
        uiPhaseLabel = new JLabel("Phase: READY");
        status.add(uiClientIdLabel);
        status.add(uiRoomLabel);
        status.add(uiPhaseLabel);
        root.add(status, BorderLayout.NORTH);

        // Log Window
        uiLog = new JTextArea();
        uiLog.setEditable(false);
        root.add(new JScrollPane(uiLog), BorderLayout.CENTER);

        // Controls Section
        JPanel controls = new JPanel(new GridLayout(4, 1));
        root.add(controls, BorderLayout.SOUTH);

        // Connect panel
        JPanel connectPanel = new JPanel();
        JTextField txtName = new JTextField("Player", 10);
        JTextField txtHost = new JTextField("localhost", 10);
        JTextField txtPort = new JTextField("3000", 5);
        JButton btnConnect = new JButton("Connect");

        connectPanel.add(new JLabel("Name:"));
        connectPanel.add(txtName);

        connectPanel.add(new JLabel("Host:"));
        connectPanel.add(txtHost);

        connectPanel.add(new JLabel("Port:"));
        connectPanel.add(txtPort);

        connectPanel.add(btnConnect);
        controls.add(connectPanel);

        btnConnect.addActionListener(e -> {
            boolean ok = uiConnect(txtHost.getText(), Integer.parseInt(txtPort.getText()), txtName.getText());
            uiLog(ok ? "Connected!" : "Connection failed.");
        });

        // Room controls
        JPanel roomPanel = new JPanel();
        JTextField txtCreate = new JTextField("game1", 10);
        JButton btnCreate = new JButton("Create");
        JTextField txtJoin = new JTextField("game1", 10);
        JButton btnJoin = new JButton("Join");

        roomPanel.add(new JLabel("Create:"));
        roomPanel.add(txtCreate);
        roomPanel.add(btnCreate);

        roomPanel.add(new JLabel("Join:"));
        roomPanel.add(txtJoin);
        roomPanel.add(btnJoin);
        controls.add(roomPanel);

        btnCreate.addActionListener(e -> {
            try { sendRoomAction(txtCreate.getText(), RoomAction.CREATE); } catch (Exception ignored) {}
            uiLog("Creating room " + txtCreate.getText());
        });
        btnJoin.addActionListener(e -> {
            try { sendRoomAction(txtJoin.getText(), RoomAction.JOIN); } catch (Exception ignored) {}
            uiLog("Joining room " + txtJoin.getText());
        });

        // Ready button
        JPanel readyPanel = new JPanel();
        JButton btnReady = new JButton("READY");
        readyPanel.add(btnReady);
        controls.add(readyPanel);

        btnReady.addActionListener(e -> {
            try { sendReady(); } catch (Exception ignored) {}
            uiLog("You pressed READY.");
        });

        // RPS buttons
        JPanel rpsPanel = new JPanel();
        JButton rock = new JButton("Rock");
        JButton paper = new JButton("Paper");
        JButton scissors = new JButton("Scissors");
        rpsPanel.add(rock); rpsPanel.add(paper); rpsPanel.add(scissors);
        controls.add(rpsPanel);

        rock.addActionListener(e -> { try { sendPick("r"); } catch (Exception ignored) {} });
        paper.addActionListener(e -> { try { sendPick("p"); } catch (Exception ignored) {} });
        scissors.addActionListener(e -> { try { sendPick("s"); } catch (Exception ignored) {} });

        uiFrame.setVisible(true);
    }

    // -------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------
    public static void main(String[] args) {
        Client c = Client.INSTANCE;
        c.buildUI();   // Start Swing UI
        try {
            c.start(); // Still supports console commands
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
