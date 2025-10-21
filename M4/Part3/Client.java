package M4.Part3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import M4.Part3.TextFX.Color;

/**
 * Client class for sending commands and messages to the server.
 * Supports commands: /connect, /reverse, /shuffle, and /quit.
 * 
 * UCID: st944
 * Date: 10/21/2025
 */
public class Client {

    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    private volatile boolean isRunning = true;

    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");

    public Client() {
        System.out.println("Client Created");
    }

    public boolean isConnected() {
        if (server == null)
            return false;
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new ObjectOutputStream(server.getOutputStream());
            in = new ObjectInputStream(server.getInputStream());
            System.out.println("Connected to server: " + address + ":" + port);
            CompletableFuture.runAsync(this::listenToServer);
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + address);
        } catch (IOException e) {
            System.out.println("I/O error while connecting: " + e.getMessage());
        }
        return isConnected();
    }

    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    private boolean processClientCommand(String text) throws IOException {
        boolean wasCommand = false;

        if (isConnection(text)) {
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            wasCommand = true;

        } else if ("/quit".equalsIgnoreCase(text)) {
            close();
            wasCommand = true;

        } else if (text.startsWith("/reverse")) {
            // UCID: st944 | Date: 10/21/2025
            String toReverse = text.replace("/reverse", "").trim();
            String[] commandData = { Constants.COMMAND_TRIGGER, "reverse", toReverse };
            sendToServer(String.join(",", commandData));
            wasCommand = true;

        } else if (text.startsWith("/shuffle")) {
            // UCID: st944 | Date: 10/21/2025
            String toShuffle = text.replace("/shuffle", "").trim();
            String[] commandData = { Constants.COMMAND_TRIGGER, "shuffle", toShuffle };
            sendToServer(String.join(",", commandData));
            wasCommand = true;
        }

        return wasCommand;
    }

    private void sendToServer(String message) throws IOException {
        if (isConnected()) {
            out.writeObject(message);
            out.flush();
        } else {
            System.out.println("Not connected to server");
        }
    }

    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                String fromServer = (String) in.readObject();
                if (fromServer != null)
                    System.out.println(TextFX.colorize(fromServer, Color.BLUE));
                else
                    break;
            }
        } catch (Exception e) {
            if (isRunning)
                System.out.println("Connection dropped: " + e.getMessage());
        } finally {
            closeServerConnection();
        }
    }

    public void start() throws IOException {
        System.out.println("Client starting...");
        try (Scanner si = new Scanner(System.in)) {
            while (isRunning) {
                System.out.print("> ");
                String line = si.nextLine();
                if (!processClientCommand(line)) {
                    sendToServer(line);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception from start()");
            e.printStackTrace();
        } finally {
            close();
        }
    }

    private void close() {
        isRunning = false;
        closeServerConnection();
        System.out.println("Client closed.");
    }

    private void closeServerConnection() {
        try {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (server != null)
                server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
