package M4.Part2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server supporting private messages (/pm)
 * UCID: st944
 * Date: 10/21/2025
 */
public class Server {
    private int port = 3000;

    // UCID: st944 | Date: 10/21/2025
    // Store connected clients using IDs
    private ConcurrentHashMap<Integer, PrintWriter> clients = new ConcurrentHashMap<>();
    private int nextId = 1;

    private void start(int port) {
        this.port = port;
        System.out.println("Listening on port " + this.port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server ready. Waiting for clients...");
            while (true) {
                Socket client = serverSocket.accept();
                int clientId = nextId++;
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                clients.put(clientId, out);
                System.out.println("Client #" + clientId + " connected.");

                // UCID: st944 | Date: 10/21/2025
                // Start new thread for each client
                new Thread(() -> handleClient(client, clientId)).start();
            }
        } catch (IOException e) {
            System.out.println("Exception from start()");
            e.printStackTrace();
        } finally {
            System.out.println("closing server socket");
        }
    }

    /**
     * UCID: st944 | Date: 10/21/2025
     * Handles private message logic
     */
    private void handlePrivateMessage(int senderId, String[] parts) {
        try {
            int targetId = Integer.parseInt(parts[1]);
            String message = String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length));
            PrintWriter targetOut = clients.get(targetId);
            PrintWriter senderOut = clients.get(senderId);

            if (targetOut != null) {
                String formatted = "Server: PM from Client#" + senderId + ": " + message;
                targetOut.println(formatted);
                senderOut.println(formatted);
                System.out.println(formatted);
            } else {
                senderOut.println("Server: Client#" + targetId + " not found.");
            }
        } catch (Exception e) {
            clients.get(senderId).println("Server: Invalid PM format. Use /pm <targetId> <message>");
        }
    }

    private void handleClient(Socket client, int clientId) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            String fromClient;
            while ((fromClient = in.readLine()) != null) {
                System.out.println("Client#" + clientId + ": " + fromClient);

                if (fromClient.startsWith("/pm ")) {
                    handlePrivateMessage(clientId, fromClient.split(" "));
                } else if ("/kill server".equalsIgnoreCase(fromClient)) {
                    System.out.println("Server shutting down by command.");
                    System.exit(0);
                } else {
                    // echo default
                    clients.get(clientId).println("Server echo: " + fromClient);
                }
            }
        } catch (IOException e) {
            System.out.println("Client#" + clientId + " disconnected.");
        } finally {
            clients.remove(clientId);
        }
    }

    public static void main(String[] args) {
        System.out.println("Server Starting");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // default
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}
