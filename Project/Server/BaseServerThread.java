// st944 – 12/07/2025
// Updated BaseServerThread for UI integration (Milestone 3).
// FIXED: onInitialized() is no longer called too early.
// It will now be called by ServerThread AFTER CLIENT_CONNECT is received.

package Project.Server;

import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.Payload;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public abstract class BaseServerThread extends Thread {

    protected Socket client;
    protected ObjectOutputStream out;
    protected ObjectInputStream in;

    protected long clientId = Constants.DEFAULT_CLIENT_ID;
    protected String clientName = "Unknown";
    protected Room currentRoom = null;

    protected Project.Common.User user = new Project.Common.User();

    private volatile boolean isRunning = true;

    // -----------------------------------------
    // Logging helpers
    // -----------------------------------------
    protected abstract void info(String message);

    protected void warn(String msg) {
        LoggerUtil.INSTANCE.warning(TextFX.colorize(msg, Color.YELLOW));
    }

    protected void err(String msg) {
        LoggerUtil.INSTANCE.severe(TextFX.colorize(msg, Color.RED));
    }

    // -----------------------------------------
    // Thread entry point
    // -----------------------------------------
    @Override
    public void run() {
        try {
            setupStreams();
            info("Streams established. Waiting for CLIENT_CONNECT...");

            // ❗ FIXED: onInitialized() REMOVED from here.
            // It must be called ONLY after CLIENT_CONNECT is processed.

            listenForPayloads();

        } catch (Exception e) {
            err("Thread crashed: " + e.getMessage());
        } finally {
            shutdownConnection();
        }
    }

    // -----------------------------------------
    // STREAM SETUP
    // -----------------------------------------
    private void setupStreams() throws IOException {
        out = new ObjectOutputStream(client.getOutputStream());
        in = new ObjectInputStream(client.getInputStream());
    }

    // -----------------------------------------
    // PAYLOAD LISTENER LOOP
    // -----------------------------------------
    private void listenForPayloads() {
        try {
            while (isRunning && !client.isClosed()) {

                Payload incoming = (Payload) in.readObject();

                if (incoming == null) {
                    warn("Received null payload — client likely disconnected.");
                    break;
                }

                processPayload(incoming);
            }
        } catch (IOException io) {
            warn("Client disconnected abruptly.");
        } catch (ClassNotFoundException e) {
            err("Invalid payload received (ClassNotFoundException).");
        } catch (Exception e) {
            err("Unexpected exception in receive loop: " + e.getMessage());
        }
    }

    // -----------------------------------------
    // SEND TO CLIENT
    // -----------------------------------------
    protected boolean sendToClient(Payload payload) {
        try {
            if (client == null || client.isClosed()) {
                warn("Attempted to send on closed socket.");
                return false;
            }

            out.writeObject(payload);
            out.flush();
            return true;

        } catch (IOException e) {
            err("Send failed, shutting down this client thread.");
            shutdownConnection();
            return false;
        }
    }

    // -----------------------------------------
    // ABSTRACT: Implemented in ServerThread
    // -----------------------------------------
    protected abstract void processPayload(Payload incoming);

    // -----------------------------------------
    // CLIENT + ROOM STATE HELPERS
    // -----------------------------------------
    public void setClientId(long id) {
        this.clientId = id;
        this.user.setClientId(id);
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientName(String name) {
        this.clientName = name;
        this.user.setClientName(name);
    }

    public String getClientName() {
        return clientName;
    }

    public void setCurrentRoom(Room room) {
        this.currentRoom = room;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public String getDisplayName() {
        return clientName + "#" + clientId;
    }

    // -----------------------------------------
    // SHUTDOWN
    // -----------------------------------------
    public void shutdownConnection() {
        isRunning = false;

        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (client != null) client.close(); } catch (Exception ignored) {}

        info("Connection closed.");
    }

    // -----------------------------------------
    // MUST be called manually from ServerThread
    // AFTER username is known (CLIENT_CONNECT)
    // -----------------------------------------
    protected abstract void onInitialized();
}
