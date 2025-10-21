package M4.Part1;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client class for sending commands and messages to the server.
 * Implements /connect, /flip, and /quit commands.
 *
 * UCID: st944
 * Date: 10/21/2025
 */
public class Client {

    private Socket server = null;
    private PrintWriter out = null;
    private Scanner inFromServer = null;
    private boolean isRunning = false;

    // Regex patterns for connection commands
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})"); // 192.168.0.2:3000
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})"); // localhost:3000

    public Client() {
        System.out.println("Client Created");
    }

    /**
     * Checks if the client socket is still valid.
     */
    public boolean isConnected() {
        if (server == null)
            return false;
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    /**
     * Connects to a server by IP/hostname and port.
     */
    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new PrintWriter(server.getOutputStream(), true);

            // UCID: st944 | Date: 10/21/2025
            // Added input stream reader and thread to display server messages
            inFromServer = new Scanner(server.getInputStream());
            System.out.println("Client connected to " + address + ":" + port);

            // UCID: st944 | Date: 10/21/2025
            new Thread(() -> {
                try {
                    while (inFromServer.hasNextLine()) {
                        String msg = inFromServer.nextLine();
                        System.out.println(msg);
                    }
                } catch (Exception e) {
                    System.out.println("Lost connection to server.");
                }
            }).start();

        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + address);
        } catch (IOException e) {
            System.out.println("I/O error while connecting: " + e.getMessage());
        }
        return isConnected();
    }

    /**
     * Checks whether text is a valid /connect command.
     */
    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    /**
     * Handles special client-side commands: /connect, /flip, /quit.
     */
    private boolean processClientCommand(String text) {

        if (isConnection(text)) {
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            return true;

        } else if ("/quit".equalsIgnoreCase(text)) {
            isRunning = false;
            return true;

        // UCID: st944 | Date: 10/21/2025
        // Added /flip command logic
        } else if ("/flip".equalsIgnoreCase(text)) {
            if (isConnected()) {
                out.println("/flip");
            } else {
                System.out.println("Not connected to server");
            }
            return true;
        }

        return false;
    }

    /**
     * Starts the client input loop.
     */
    public void start() throws IOException {
        System.out.println("Client starting...");
        try (Scanner si = new Scanner(System.in)) {
            String line = "";
            isRunning = true;

            while (isRunning) {
                try {
                    System.out.print("> ");
                    line = si.nextLine();
                    if (!processClientCommand(line)) {
                        if (isConnected()) {
                            out.println(line);
                            if (out.checkError()) {
                                System.out.println("Connection to server may have been lost.");
                            }
                        } else {
                            System.out.println("Not connected to server");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Connection dropped");
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception from start()");
            e.printStackTrace();
        } finally {
            close();
        }
    }

    /**
     * Gracefully closes the client connection.
     */
    private void close() {
        try {
            if (out != null) {
                System.out.println("Closing output stream");
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (server != null) {
                System.out.println("Closing connection");
                server.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Client closed.");
    }

    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.start();
        } catch (IOException e) {
            System.out.println("Exception from main()");
            e.printStackTrace();
        }
    }
}
