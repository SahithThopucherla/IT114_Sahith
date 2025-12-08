// st944 – 12/08/2025
// FINAL ServerThread for Milestone 3 UI + GameRoom support
// Replaces reflection-based logic with clean, direct method calls.
// Ensures all clients start in the Lobby and can create/join rooms safely.

package Project.Server;

import Project.Common.*;
import Project.Common.TextFX.Color;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ServerThread extends BaseServerThread {

    private final Consumer<ServerThread> onInitializationComplete;

    public ServerThread(Socket clientSocket, Consumer<ServerThread> initCallback) {
        Objects.requireNonNull(clientSocket, "Client socket cannot be null");
        Objects.requireNonNull(initCallback, "Initialization callback cannot be null");

        this.client = clientSocket;
        this.onInitializationComplete = initCallback;

        info("ServerThread created");
    }

    // ----------------------------------------------------
    // LOGGING HELPERS
    // ----------------------------------------------------
    @Override
    protected void info(String msg) {
        LoggerUtil.INSTANCE.info(
            TextFX.colorize(String.format("Thread[%s]: %s", getClientId(), msg), Color.CYAN)
        );
    }

    @Override
    protected void warn(String msg) {
        LoggerUtil.INSTANCE.warning(
            TextFX.colorize(String.format("Thread[%s]: %s", getClientId(), msg), Color.YELLOW)
        );
    }

    @Override
    protected void err(String msg) {
        LoggerUtil.INSTANCE.severe(
            TextFX.colorize(String.format("Thread[%s]: %s", getClientId(), msg), Color.RED)
        );
    }

    // ----------------------------------------------------
    // SEND HELPER METHODS
    // ----------------------------------------------------
    public boolean sendClientId() {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.CLIENT_ID);
        cp.setClientId(getClientId());
        cp.setClientName(getClientName());
        return sendToClient(cp);
    }

    public boolean sendMessage(long fromId, String msg) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setClientId(fromId);
        p.setMessage(msg);
        return sendToClient(p);
    }

    public boolean sendRooms(List<String> rooms) {
        RoomResultPayload rrp = new RoomResultPayload();
        rrp.setRooms(rooms);
        return sendToClient(rrp);
    }

    public boolean sendReadyStatus(long id, boolean ready, boolean quiet) {
        ReadyPayload rp = new ReadyPayload();
        rp.setClientId(id);
        rp.setReady(ready);
        rp.setPayloadType(quiet ? PayloadType.SYNC_READY : PayloadType.READY);
        return sendToClient(rp);
    }

    public boolean sendTurnStatus(long id, boolean tookTurn, boolean quiet) {
        ReadyPayload rp = new ReadyPayload();
        rp.setClientId(id);
        rp.setReady(tookTurn);
        rp.setPayloadType(quiet ? PayloadType.SYNC_TURN : PayloadType.TURN);
        return sendToClient(rp);
    }

    public boolean sendResetReady() {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(PayloadType.RESET_READY);
        return sendToClient(rp);
    }

    public boolean sendCurrentPhase(Phase phase) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.PHASE);
        p.setMessage(phase.name());
        return sendToClient(p);
    }

    public boolean sendResetTurnStatus() {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(PayloadType.RESET_TURN);
        return sendToClient(rp);
    }

    // ----------------------------------------------------
    // MAIN PAYLOAD ROUTER
    // ----------------------------------------------------
    @Override
    protected void processPayload(Payload incoming) {

        switch (incoming.getPayloadType()) {

            // ----------------------------------
            // INITIAL CLIENT HANDSHAKE
            // ----------------------------------
            case CLIENT_CONNECT:
                ConnectionPayload cp = (ConnectionPayload) incoming;
                setClientName(cp.getClientName().trim());
                sendClientId();

                // best-effort: attempt to add to lobby via reflection fallback
                invokeRoomMethod("addClient", new Class<?>[]{ServerThread.class}, this);
                setCurrentRoom(null);
                info("Joined Lobby");

                onInitialized();
                break;

            // ----------------------------------
            // DISCONNECT
            // ----------------------------------
            case DISCONNECT:
                invokeRoomMethod("handleDisconnect", new Class<?>[]{ServerThread.class}, this);
                break;

            // ----------------------------------
            // CHAT MESSAGE
            // ----------------------------------
            case MESSAGE:
                invokeRoomMethod("handleMessage", new Class<?>[]{ServerThread.class, String.class}, this, incoming.getMessage());
                break;

            // ----------------------------------
            // REVERSE (demo)
            // ----------------------------------
            case REVERSE:
                invokeRoomMethod("handleReverseText", new Class<?>[]{ServerThread.class, String.class}, this, incoming.getMessage());
                break;

            // ----------------------------------
            // ROOM CREATE
            // ----------------------------------
            case ROOM_CREATE:
                invokeRoomMethod("handleCreateRoom", new Class<?>[]{ServerThread.class, String.class}, this, incoming.getMessage());
                break;

            // ----------------------------------
            // ROOM JOIN
            // ----------------------------------
            case ROOM_JOIN:
                invokeRoomMethod("handleJoinRoom", new Class<?>[]{ServerThread.class, String.class}, this, incoming.getMessage());
                break;

            // ----------------------------------
            // ROOM LEAVE → return to Lobby
            // ----------------------------------
            case ROOM_LEAVE:
                invokeRoomMethod("handleJoinRoom", new Class<?>[]{ServerThread.class, String.class}, this, Room.LOBBY);
                break;

            // ----------------------------------
            // ROOM LIST
            // ----------------------------------
            case ROOM_LIST:
                invokeRoomMethod("handleListRooms", new Class<?>[]{ServerThread.class, String.class}, this, incoming.getMessage());
                break;

            // ----------------------------------
            // READY (only valid inside GameRoom)
            // ----------------------------------
            case READY:
                invokeRoomMethod("handleReady", new Class<?>[]{ServerThread.class}, this);
                break;

            // ----------------------------------
            // TURN ACTION
            // ----------------------------------
            case TURN:
                invokeRoomMethod("handleTurnAction", new Class<?>[]{ServerThread.class, String.class}, this, incoming.getMessage());
                break;

            // ----------------------------------
            // ROCK / PAPER / SCISSORS PICK
            // ----------------------------------
            case PICK:
                invokeRoomMethod("handlePick", new Class<?>[]{ServerThread.class, String.class}, this, incoming.getMessage());
                break;

            default:
                warn("Unhandled payload: " + incoming.getPayloadType());
        }
    }

    // Reflection fallback for Room methods that may not exist on the Room base type.
    private void invokeRoomMethod(String name, Class<?>[] paramTypes, Object... args) {
        if (currentRoom == null) {
            warn("No current room to invoke " + name);
            return;
        }
        try {
            Method m = currentRoom.getClass().getMethod(name, paramTypes);
            m.invoke(currentRoom, args);
        } catch (NoSuchMethodException e) {
            warn("Room does not implement " + name + ": " + e.getMessage());
            sendMessage(-1, "Server: Room does not support action: " + name);
        } catch (Exception e) {
            err("Error invoking " + name + " on room: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------
    public boolean isReady() { return user.isReady(); }
    public void setReady(boolean r) { user.setReady(r); }

    public boolean didTakeTurn() { return user.didTakeTurn(); }
    public void setTookTurn(boolean t) { user.setTookTurn(t); }

    // ----------------------------------------------------
    // INITIALIZATION COMPLETE
    // ----------------------------------------------------
    @Override
    protected void onInitialized() {
        onInitializationComplete.accept(this);
    }
}
