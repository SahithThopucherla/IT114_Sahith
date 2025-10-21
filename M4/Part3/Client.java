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
 * Demoing bi-directional communication between client and server in a
 * multi-client scenario
 */
public class Client {

    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");
    private volatile boolean isRunning = true; // volatile for thread-safe visibility

    public Client() {
        System.out.println("Client Created");
    }

    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine if the server had a problem
        // and is just for lesson's sake
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    /**
     * Takes an IP address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @return true if connection was successful
     */
    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            System.out.println("Client connected");
            // Use CompletableFuture to run listenToServer() in a separate thread
            CompletableFuture.runAsync(this::listenToServer);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    /**
     * <p>
     * Check if the string contains the <i>connect</i> command
     * followed by an IP address and port or localhost and port.
     * </p>
     * <p>
     * Example format: 123.123.123.123:3000
     * </p>
     * <p>
     * Example format: localhost:3000
     * </p>
     * https://www.w3schools.com/java/java_regex.asp
     * 
     * @param text
     * @return true if the text is a valid connection command
     */
    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    /**
     * Controller for handling various text commands.
     * <p>
     * Add more here as needed
     * </p>
     * 
     * @param text
     * @return true if the text was a command or triggered a command
     * @throws IOException
     */
    private boolean processClientCommand(String text) throws IOException {
        boolean wasCommand = false;
        if (isConnection(text)) {
            // replaces multiple spaces with a single space
            // splits on the space after connect (gives us host and port)
            // splits on : to get host as index 0 and port as index 1
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            wasCommand = true;
        } else if ("/quit".equalsIgnoreCase(text)) {
            close();
            wasCommand = true;
        } else if ("/disconnect".equalsIgnoreCase(text)) {
            // index 0 = trigger, index 1 = command, index N = command data
            String[] commandData = { Constants.COMMAND_TRIGGER, "disconnect" };
            sendToServer(String.join(",", commandData));
            wasCommand = true;
        } else if (text.startsWith("/reverse")) {
            text = text.replace("/reverse", "").trim();
            // index 0 = trigger, index 1 = command, index N = command data
            String[] commandData = { Constants.COMMAND_TRIGGER, "reverse", text };
            sendToServer(String.join(",", commandData));
            wasCommand = true;
        }
        return wasCommand;
    }

    public void start() throws IOException {
        System.out.println("Client starting");

        // Use CompletableFuture to run listenToInput() in a separate thread
        CompletableFuture<Void> inputFuture = CompletableFuture.runAsync(this::listenToInput);

        // Wait for inputFuture to complete to ensure proper termination
        inputFuture.join();
    }

    /**
     * Listens for messages from the server
     */
    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                String fromServer = (String) in.readObject(); // blocking read
                if (fromServer != null) {
                    System.out.println(TextFX.colorize(fromServer, Color.BLUE));
                } else {
                    System.out.println("Server disconnected");
                    break;
                }
            }
        } catch (ClassCastException | ClassNotFoundException cce) {
            System.err.println("Error reading object as specified type: " + cce.getMessage());
            cce.printStackTrace();
        } catch (IOException e) {
            if (isRunning) {
                System.out.println("Connection dropped");
                e.printStackTrace();
            }
        } finally {
            closeServerConnection();
        }
        System.out.println("listenToServer thread stopped");
    }

    /**
     * Listens for keyboard input from the user
     */
    private void listenToInput() {
        try (Scanner si = new Scanner(System.in)) {
            System.out.println("Waiting for input"); // moved here to avoid console spam
            while (isRunning) { // Run until isRunning is false
                String userInput = si.nextLine();
                if (!processClientCommand(userInput)) {
                    sendToServer(userInput);
                }
            }
        } catch (IOException ioException) {
            System.out.println("Error in listentToInput()");
            ioException.printStackTrace();
        }
        System.out.println("listenToInput thread stopped");
    }

    private void sendToServer(String message) throws IOException {
        if (isConnected()) {
            out.writeObject(message);
            out.flush(); // good practice to ensure data is written out immediately
        } else {
            System.out.println(
                    "Not connected to server (hint: type `/connect host:port` without the quotes and replace host/port with the necessary info)");
        }
    }

    /**
     * Closes the client connection and associated resources
     */
    private void close() {
        isRunning = false;
        closeServerConnection();
        System.out.println("Client terminated");
        // System.exit(0); // Terminate the application
    }

    /**
     * Closes the server connection and associated resources
     */
    private void closeServerConnection() {
        try {
            if (out != null) {
                System.out.println("Closing output stream");
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (in != null) {
                System.out.println("Closing input stream");
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (server != null) {
                System.out.println("Closing connection");
                server.close();
                System.out.println("Closed socket");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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