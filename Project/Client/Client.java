package Project.Client;

import Project.Common.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public enum Client {
    INSTANCE;

    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private volatile boolean isRunning = true;

    private final ConcurrentHashMap<Long, User> knownClients = new ConcurrentHashMap<>();
    private User myUser = new User();
    private Phase currentPhase = Phase.READY;

    // UI callback handler
    private ClientUi ui;

    public void setUi(ClientUi ui) {
        this.ui = ui;
    }

    // ============================================================
    // CONNECTION
    // ============================================================

    public boolean isConnected() {
        return server != null &&
                server.isConnected() &&
                !server.isClosed() &&
                !server.isInputShutdown() &&
                !server.isOutputShutdown();
    }

    public boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new ObjectOutputStream(server.getOutputStream());
            in = new ObjectInputStream(server.getInputStream());

            log("Connected to server");
            CompletableFuture.runAsync(this::listenToServer);
            return true;

        } catch (Exception e) {
            error("Could not connect: " + e.getMessage());
            return false;
        }
    }

    // ============================================================
    // SEND TO SERVER
    // ============================================================

    private void sendToServer(Payload payload) throws IOException {
        if (!isConnected()) {
            error("Not connected to server");
            return;
        }
        out.writeObject(payload);
        out.flush();
    }

    public void sendClientName(String name) {
        try {
            ConnectionPayload cp = new ConnectionPayload();
            cp.setPayloadType(PayloadType.CLIENT_CONNECT);
            cp.setClientName(name);
            sendToServer(cp);
        } catch (Exception e) {
            error("Failed to send username");
        }
    }

    public void sendReady() {
        try {
            ReadyPayload rp = new ReadyPayload();
            rp.setPayloadType(PayloadType.READY);
            sendToServer(rp);
        } catch (Exception e) {
            error("Could not send READY");
        }
    }

    public void sendPick(String choice) {
        try {
            Payload p = new Payload();
            p.setPayloadType(PayloadType.PICK);
            p.setMessage(choice);
            sendToServer(p);
        } catch (Exception e) {
            error("Could not send pick");
        }
    }

    // ============================================================
    // UI-FRIENDLY ROOM ACTION HELPERS  (used by ConnectionPanel)
    // ============================================================

    public void sendRoomActionFromUi(String roomName, RoomAction action) {
        try {
            Payload p = new Payload();
            p.setMessage(roomName);

            switch (action) {
                case CREATE:
                    p.setPayloadType(PayloadType.ROOM_CREATE);
                    break;
                case JOIN:
                    p.setPayloadType(PayloadType.ROOM_JOIN);
                    break;
                case LEAVE:
                    p.setPayloadType(PayloadType.ROOM_LEAVE);
                    break;
                case LIST:
                    p.setPayloadType(PayloadType.ROOM_LIST);
                    break;
            }

            sendToServer(p);
            log("Room action: " + action + " (" + roomName + ")");

        } catch (Exception e) {
            error("Failed: " + e.getMessage());
        }
    }

    public void createRoomFromUi(String roomName) {
        sendRoomActionFromUi(roomName, RoomAction.CREATE);
    }

    public void joinRoomFromUi(String roomName) {
        sendRoomActionFromUi(roomName, RoomAction.JOIN);
    }

    public void leaveRoomFromUi() {
        sendRoomActionFromUi("", RoomAction.LEAVE);
    }

    // ============================================================
    // SERVER LISTENER
    // ============================================================

    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                Payload incoming = (Payload) in.readObject();
                if (incoming != null) {
                    processPayload(incoming);
                }
            }
        } catch (Exception e) {
            error("Connection lost");
        } finally {
            closeConnection();
        }
    }

    // ============================================================
    // PAYLOAD ROUTING
    // ============================================================

    private void processPayload(Payload p) {

        switch (p.getPayloadType()) {

            case CLIENT_ID:
                processClientId(p);
                break;

            case MESSAGE:
                ui.onLogMessage(p.getMessage());
                break;

            case ROOM_JOIN:
            case ROOM_LEAVE:
            case SYNC_CLIENT:
                processRoomUpdate(p);
                break;

            case PHASE:
                currentPhase = Phase.valueOf(p.getMessage());
                ui.onPhaseChanged(currentPhase.name());
                break;

            case READY:
            case SYNC_READY:
                processReadyStatus((ReadyPayload)p);
                break;

            case TURN:
            case SYNC_TURN:
                processTurnStatus((ReadyPayload)p);
                break;

            default:
                ui.onLogMessage("Unhandled: " + p.getPayloadType());
        }
    }

    // ============================================================
    // PAYLOAD HANDLERS
    // ============================================================

    private void processClientId(Payload p) {
        myUser.setClientId(p.getClientId());
        myUser.setClientName(((ConnectionPayload)p).getClientName());
        knownClients.put(myUser.getClientId(), myUser);

        ui.onConnected(myUser.getClientId());
    }

    private void processRoomUpdate(Payload p) {
        ConnectionPayload cp = (ConnectionPayload)p;

        if (cp.getPayloadType() == PayloadType.ROOM_JOIN) {
            ui.onRoomChanged(cp.getMessage());
        }

        if (cp.getClientId() != Constants.DEFAULT_CLIENT_ID) {
            User u = new User();
            u.setClientId(cp.getClientId());
            u.setClientName(cp.getClientName());
            knownClients.put(cp.getClientId(), u);
        }
    }

    private void processReadyStatus(ReadyPayload rp) {
        User u = knownClients.get(rp.getClientId());
        if (u != null) u.setReady(rp.isReady());
    }

    private void processTurnStatus(ReadyPayload rp) {
        User u = knownClients.get(rp.getClientId());
        if (u != null) u.setTookTurn(rp.isReady());
    }

    // ============================================================
    // CLOSE
    // ============================================================

    private void closeConnection() {
        try { isRunning = false; } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (server != null) server.close(); } catch (Exception ignored) {}

        log("Disconnected.");
    }

    // ============================================================
    // LOGGING HELPERS
    // ============================================================

    private void log(String msg) {
        if (ui != null) ui.onLogMessage(msg);
    }

    private void error(String msg) {
        if (ui != null) ui.onLogMessage("ERROR: " + msg);
    }

    // ============================================================
    // MAIN (UI Launcher)
    // ============================================================

    public static void main(String[] args) {
        RpsClientFrame frame = new RpsClientFrame(Client.INSTANCE);
        frame.setVisible(true);
    }
}
