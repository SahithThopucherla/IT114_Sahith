// st944 – 12/08/2025
// FINAL Room.java for Milestone 3 RPS
// Base abstract room that all rooms (Lobby, GameRoom) extend.
// Manages room membership, creation, joining, leaving, and communication.

package Project.Server;

import Project.Common.Constants;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Room {

    // Name of this room ("lobby", "game", etc.)
    protected final String name;

    // All clients currently inside the room
    protected final ConcurrentHashMap<Long, ServerThread> clientsInRoom = new ConcurrentHashMap<>();

    // GLOBAL ROOM REGISTRY
    private static final Map<String, Room> ALL_ROOMS = new ConcurrentHashMap<>();

    // Constant for lobby name
    public static final String LOBBY = "lobby";

    // -------------------------------------------------------
    // CONSTRUCTOR: Registers room into global registry
    // -------------------------------------------------------
    protected Room(String name) {
        this.name = name.toLowerCase();
        ALL_ROOMS.put(this.name, this);
    }

    public String getName() {
        return name;
    }

    // -------------------------------------------------------
    // STATIC ROOM OPERATIONS
    // -------------------------------------------------------
    public static Room getRoom(String name) {
        if (name == null) return null;
        return ALL_ROOMS.get(name.toLowerCase());
    }

    public static Map<String, Room> getAllRooms() {
        return ALL_ROOMS;
    }

    public static boolean exists(String name) {
        return ALL_ROOMS.containsKey(name.toLowerCase());
    }

    // Create Lobby once
    public static void ensureLobbyExists() {
        if (!exists(LOBBY)) {
            // Use GameRoom for the lobby to avoid referencing a missing LobbyRoom class
            new GameRoom(LOBBY); // auto-registers itself
        }
    }

    // -------------------------------------------------------
    // CLIENT ENTER / EXIT MANAGEMENT
    // -------------------------------------------------------
    public synchronized void addClient(ServerThread client) {
        clientsInRoom.put(client.getClientId(), client);
        client.setCurrentRoom(this);

        onClientAdded(client);
    }

    public synchronized void removeClient(ServerThread client) {
        clientsInRoom.remove(client.getClientId());
        onClientRemoved(client);
    }

    // Broadcast helper
    protected synchronized void broadcast(String msg) {
        clientsInRoom.values().forEach(c ->
                c.sendMessage(Constants.DEFAULT_CLIENT_ID, msg));
    }

    // -------------------------------------------------------
    // REQUIRED BEHAVIOR FOR ALL ROOMS
    // -------------------------------------------------------
    protected abstract void onClientAdded(ServerThread client);

    protected abstract void onClientRemoved(ServerThread client);

    // -------------------------------------------------------
    // ROOM CREATION HANDLER
    // Called when a client sends ROOM_CREATE
    // -------------------------------------------------------
    public synchronized void handleCreateRoom(ServerThread sender, String roomName) {
        roomName = roomName.toLowerCase();

        if (exists(roomName)) {
            sender.sendMessage(-1, "Room already exists: " + roomName);
            return;
        }

        // Create new GameRoom
        Room newRoom = new GameRoom(roomName);

        sender.sendMessage(-1, "Created room: " + roomName);

        // Auto-join immediately
        handleJoinRoom(sender, roomName);
    }

    // -------------------------------------------------------
    // JOIN ROOM HANDLER
    // Called when a client sends ROOM_JOIN
    // -------------------------------------------------------
    public synchronized void handleJoinRoom(ServerThread sender, String roomName) {
        roomName = roomName.toLowerCase();

        Room target = getRoom(roomName);
        if (target == null) {
            sender.sendMessage(-1, "Room does not exist: " + roomName);
            return;
        }

        // Leave existing room first
        if (sender.getCurrentRoom() != null) {
            sender.getCurrentRoom().removeClient(sender);
        }

        // Enter new room
        target.addClient(sender);
        sender.sendMessage(-1, "Joined room: " + target.getName());
    }

    // -------------------------------------------------------
    // LIST ROOMS
    // Called when the user sends ROOM_LIST
    // -------------------------------------------------------
    public synchronized void handleListRooms(ServerThread sender, String unused) {
        StringBuilder sb = new StringBuilder("Available rooms:\n");

        for (String r : ALL_ROOMS.keySet()) {
            sb.append(" • ").append(r).append("\n");
        }

        sender.sendMessage(Constants.DEFAULT_CLIENT_ID, sb.toString());
    }
}
