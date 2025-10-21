package M4.Part1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * Server for handling multiple clients and supporting the /flip command.
 *
 * UCID: st944
 * Date: 10/21/2025
 */
public class Server {
    private int port = 3000;
    private List<PrintWriter> clientOutputs = new ArrayList<>();
    private Random random = new Random();

    /**
     * Handles the coin flip command.
     * @param who The name or identifier of the user (currently defaults to "Client")
     * @return A message like "Server: Client flipped a coin and got Heads"
     *
     * UCID: st944 | Date: 10/21/2025
     */
    public String flipCoin(String who) {
        String result = random.nextBoolean() ? "Heads" : "Tails";
        return "Server: " + who + " flipped a coin and got " + result;
    }

    /**
     * Sends a message to all connected clients.
     * UCID: st944 | Date: 10/21/2025
     */
    public synchronized void broadcast(String message) {
        for (PrintWriter writer : clientOutputs) {
            writer.println(message);
        }
        System.out.println(message); // Also log it on the server console
    }

    /**
     * Starts the server, waits for clients, and listens for messages.
     * UCID: st944 | Date: 10/21/2025
     */
    private void start(int port) {
        this.port = port;
        System.out.println("Listening on port " + this.port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server ready. Waiting for a client...");

            // UCID: st944 | Date: 10/21/2025
            // Supports multiple clients using separate threads
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Client connected from " + client.getInetAddress().getHostAddress());

                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                clientOutputs.add(out);

                new Thread(() -> handleClient(client, out)).start();
            }
        } catch (IOException e) {
            System.out.println("Exception from start()");
            e.printStackTrace();
        } finally {
            System.out.println("Closing server socket");
        }
    }

    /**
     * Handles messages from an individual client.
     *
     * UCID: st944 | Date: 10/21/2025
     */
    private void handleClient(Socket client, PrintWriter out) {
        String clientName = "Client"; 
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            String fromClient;
            while ((fromClient = in.readLine()) != null) {
                if ("/kill server".equalsIgnoreCase(fromClient)) {
                    System.out.println("Client requested server shutdown.");
                    broadcast("Server: Shutting down as requested by client.");
                    System.exit(0);

                // UCID: st944 | Date: 10/21/2025
                // Handles the /flip command and sends result to all clients
                } else if ("/flip".equalsIgnoreCase(fromClient)) {
                    String message = flipCoin(clientName);
                    broadcast(message);

                } else {
                    System.out.println("From client: " + fromClient);
                    broadcast(clientName + ": " + fromClient);
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Server Starting");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // Default port used if args missing or invalid
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}
