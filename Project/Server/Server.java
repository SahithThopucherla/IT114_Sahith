// st944 – 12/07/2025
// Final Server.java with LoggerUtil initialization fix
// Supports GameRoom creation, joining, lobby management, and UI integration.

package Project.Server;

import Project.Common.LoggerUtil;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private ServerSocket server;
    private volatile boolean isRunning = true;

    // Track all connected clients
    private ConcurrentHashMap<Long, ServerThread> clients = new ConcurrentHashMap<>();

    private long clientCounter = 1; // Assign unique ID to each connecting client

    // -----------------------------------------------------------
    // SERVER ENTRY POINT
    // -----------------------------------------------------------
    public static void main(String[] args) {
        Server s = new Server();
        s.start(3000);
    }

    // -----------------------------------------------------------
    // START SERVER
    // -----------------------------------------------------------
    public void start(int port) {
        try {
            // Initialize logger BEFORE first use by invoking private setupLogger via reflection (safe fallback)
            try {
                java.lang.reflect.Method setup = LoggerUtil.class.getDeclaredMethod("setupLogger");
                setup.setAccessible(true);
                setup.invoke(LoggerUtil.INSTANCE);
            } catch (Exception e) {
                // If reflection fails, fall back to stderr so we still get diagnostic output
                System.err.println("Failed to initialize LoggerUtil: " + e.getMessage());
            }

            LoggerUtil.INSTANCE.info(
                    TextFX.colorize("Starting server on port " + port, Color.GREEN)
            );

            // Ensure lobby exists before clients join
            Room.ensureLobbyExists();

            server = new ServerSocket(port);

            LoggerUtil.INSTANCE.info(
                    TextFX.colorize("Server running. Waiting for clients...", Color.GREEN)
            );

            // MAIN ACCEPT LOOP
            while (isRunning) {
                Socket clientSocket = server.accept();
                LoggerUtil.INSTANCE.info("Client connected.");
                createClientThread(clientSocket);
            }

        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Server crashed: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------
    // CREATE NEW CLIENT THREAD
    // -----------------------------------------------------------
    private void createClientThread(Socket socket) {
        long assignedId = clientCounter++;

        ServerThread thread = new ServerThread(socket, (ServerThread st) -> {
            // This callback runs once the client sends CLIENT_CONNECT
            st.setClientId(assignedId);
            clients.put(assignedId, st);

            LoggerUtil.INSTANCE.info("Client " + st.getDisplayName() + " initialized.");

            // Start client inside the Lobby
            Room lobby = null;
            try {
                // Ensure lobby exists (keeps existing behavior)
                Room.ensureLobbyExists();

                // Try to call a public static getLobby() via reflection if present
                try {
                    java.lang.reflect.Method gm = Room.class.getMethod("getLobby");
                    lobby = (Room) gm.invoke(null);
                } catch (NoSuchMethodException nsme) {
                    // Fallback: attempt to read a static 'lobby' field if getLobby() doesn't exist
                    try {
                        java.lang.reflect.Field lf = Room.class.getDeclaredField("lobby");
                        lf.setAccessible(true);
                        lobby = (Room) lf.get(null);
                    } catch (NoSuchFieldException | IllegalAccessException ex) {
                        LoggerUtil.INSTANCE.severe("Could not obtain lobby via reflection: " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                LoggerUtil.INSTANCE.severe("Failed to access lobby: " + ex.getMessage());
            }

            if (lobby == null) {
                LoggerUtil.INSTANCE.severe("Lobby not available; client " + st.getDisplayName() + " cannot be added.");
                return;
            }

            lobby.addClient(st);
            st.setCurrentRoom(lobby);

            LoggerUtil.INSTANCE.info("Client " + st.getDisplayName() + " joined lobby.");
        });

        thread.start();
    }

    // -----------------------------------------------------------
    // STOP SERVER
    // -----------------------------------------------------------
    public void stop() {
        isRunning = false;

        try {
            if (server != null) server.close();
        } catch (Exception ignored) {}

        LoggerUtil.INSTANCE.info("Server stopped.");
    }
}
