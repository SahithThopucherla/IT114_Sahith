package M4.Part3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Server class that handles incoming client connections
 * and manages broadcast and command logic.
 *
 * UCID: st944
 * Date: 10/21/2025
 */
public class Server {

    private int port = 3000;
    private boolean isRunning = false;
    private final List<ServerThread> clients = new CopyOnWriteArrayList<>();

    public Server() {
    }

    public void start(int port) {
        this.port = port;
        isRunning = true;
        System.out.println("Server listening on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                ServerThread clientThread =
                        new ServerThread(clientSocket, this, this::onClientInitialized);
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Exception from start(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    private void onClientInitialized(ServerThread client) {
        clients.add(client);
        System.out.println("Client[" + client.getClientId() + "] added. Total clients: " + clients.size());
    }

    public void stop() {
        isRunning = false;
        System.out.println("Stopping server...");
        for (ServerThread client : clients) {
            client.disconnect();
        }
        clients.clear();
    }

    protected void handleDisconnect(ServerThread client) {
        System.out.println("Client[" + client.getClientId() + "] disconnected.");
        clients.remove(client);
    }

    /**
     * Broadcasts a message to all clients.
     */
    protected synchronized void broadcast(String message) {
        System.out.println("Broadcasting: " + message);
        for (ServerThread client : clients) {
            client.sendToClient(message);
        }
    }

    /**
     * UCID: st944 | Date: 10/21/2025
     * Handles normal text messages sent from a client.
     */
    protected void handleMessage(ServerThread sender, String message) {
        String formatted = String.format("User[%s]: %s", sender.getClientId(), message);
        broadcast(formatted);
    }

    /**
     * UCID: st944 | Date: 10/21/2025
     * Handles /reverse command logic.
     */
    protected void handleReverseText(ServerThread sender, String text) {
        String reversed = new StringBuilder(text).reverse().toString();
        String formatted = String.format("Reversed from User[%s]: %s", sender.getClientId(), reversed);
        broadcast(formatted);
    }

    /**
     * UCID: st944 | Date: 10/21/2025
     * Handles /shuffle command logic.
     * Randomizes the letters of the given message and sends to everyone.
     */
    protected void handleShuffleMessage(ServerThread sender, String text) {
        if (Objects.isNull(text) || text.isEmpty()) {
            sender.sendToClient("Server: Nothing to shuffle.");
            return;
        }

        List<Character> chars = text.chars()
                                    .mapToObj(c -> (char) c)
                                    .collect(Collectors.toList());
        Collections.shuffle(chars);
        String shuffled = chars.stream()
                               .map(String::valueOf)
                               .collect(Collectors.joining(""));

        String formatted = String.format("Shuffled from User[%s]: %s",
                sender.getClientId(), shuffled);
        broadcast(formatted);
    }

    public static void main(String[] args) {
        System.out.println("Server Starting...");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // keep default port
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}
