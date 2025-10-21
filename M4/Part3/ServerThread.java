package M4.Part3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import M4.Part3.TextFX.Color;

/**
 * A server-side representation of a single client
 */
public class ServerThread extends Thread {
    private Socket client; // communication directly to "my" client
    private boolean isRunning = false; // control variable to stop this thread
    private ObjectOutputStream out; // exposed here for send()
    private Server server;// ref to our server so we can call methods on it
    // more easily
    private long clientId;
    private Consumer<ServerThread> onInitializationComplete; // callback to inform when this object is ready

    /**
     * A wrapper method so we don't need to keep typing out the long/complex sysout
     * line inside
     * 
     * @param message
     */
    private void info(String message) {
        System.out.println(String.format("Thread[%s]: %s", this.getClientId(), message));
    }

    /**
     * Returns the status of this ServerThread
     * 
     * @return
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Wraps the Socket connection and takes a Server reference and a callback
     * 
     * @param myClient
     * @param server
     * @param onInitializationComplete method to inform listener that this object is
     *                                 ready
     */
    protected ServerThread(Socket myClient, Server server, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(server, "Server cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        info("ServerThread created");
        // get communication channels to single client
        this.client = myClient;
        this.server = server; // In the future we'll have a different way to reference the Server
        this.clientId = this.threadId(); // An id associated with the thread instance, used as a temporary identifier
        this.onInitializationComplete = onInitializationComplete;

    }

    public long getClientId() {
        // Note: We return clientId instead of threadId as we'll change this identifier
        // in the future
        return this.clientId;
    }

    /**
     * One of the two ways to get this to exit the listen loop
     */
    protected void disconnect() {
        if (!isRunning) {
            // prevent multiple triggers if this gets called consecutively
            return;
        }
        info("Thread being disconnected by server");
        isRunning = false;
        this.interrupt(); // breaks out of blocking read in the run() method
        cleanup(); // good practice to ensure data is written out immediately
    }

    /**
     * Sends the message over the socket
     * 
     * @param message
     * @return true if no errors were encountered
     */
    protected boolean sendToClient(String message) {
        if (!isRunning) {
            return false;
        }
        try {
            out.writeObject(message);
            out.flush();
            return true;
        } catch (IOException e) {
            info("Error sending message to client (most likely disconnected)");
            // comment this out to inspect the stack trace
            // e.printStackTrace();
            cleanup();
            return false;
        }
    }

    @Override
    public void run() {
        info("Thread starting");
        try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());) {
            this.out = out;
            isRunning = true;
            onInitializationComplete.accept(this); // Notify server that initialization is complete
            String fromClient;
            /**
             * isRunning is a flag to let us manage the loop exit condition
             * fromClient (in.readObject()) is a blocking method that waits until data is
             * received
             * - null would likely mean a disconnect so we use a "set and check" logic to
             * alternatively exit the loop
             */
            while (isRunning) {
                try {
                    fromClient = (String) in.readObject(); // blocking method
                    if (fromClient == null) {
                        throw new IOException("Connection interrupted"); // Specific exception for a clean break
                    } else {
                        info(TextFX.colorize("Received from my client: " + fromClient, Color.CYAN));
                        processPayload(fromClient);
                    }
                } catch (ClassCastException | ClassNotFoundException cce) {
                    System.err.println("Error reading object as specified type: " + cce.getMessage());
                    cce.printStackTrace();
                } catch (IOException e) {
                    if (Thread.currentThread().isInterrupted()) {
                        info("Thread interrupted during read (likely from the disconnect() method)");
                        break;
                    }
                    info("IO exception while reading from client");
                    e.printStackTrace();
                    break;
                }
            } // close while loop
        } catch (Exception e) {
            // happens when client disconnects
            info("General Exception");
            e.printStackTrace();
            info("My Client disconnected");
        } finally {
            isRunning = false;
            info("Exited thread loop. Cleaning up connection");
            cleanup();
        }
    }

    private void processPayload(String incoming) {
        if (!processCommand(incoming)) {
            // if not command; send message to all clients via Server
            server.handleMessage(this, incoming);
        }

    }

    /**
     * Attempts to see if the message is a command and process its action
     * 
     * @param message
     * @param sender
     * @return true if it was a command, false otherwise
     */
    private boolean processCommand(String message) {
        boolean wasCommand = false; // control var to use as the return status

        // using "[cmd]" as a temporary trigger until we update how the data is passed
        // over the socket
        if (message.startsWith(Constants.COMMAND_TRIGGER)) {
            // expected format will be csv for now to keep it simple
            String[] commandData = message.split(",");
            if (commandData.length >= 2) {

                // index 0 is the trigger word
                // index 1 is the command
                final String command = commandData[1].trim();
                System.out.println(TextFX.colorize("Checking command: " + command, Color.YELLOW));
                // index N are the data from the command
                // Note: not all commands require data, some are simply actions/triggers to
                // process like quit
                switch (command) {
                    case "quit":
                    case "disconnect":
                    case "logout":
                    case "logoff":
                        server.handleDisconnect(this);
                        wasCommand = true;
                        break;
                    case "reverse":
                        // ignore the first two indexes (trigger, command)
                        String relevantText = String.join(" ", Arrays.copyOfRange(commandData, 2, commandData.length));
                        server.handleReverseText(this, relevantText);
                        wasCommand = true;
                        break;
                    // added more cases/breaks as needed for other commands
                    default:
                        break;
                }
            }

        }
        return wasCommand;
    }

    private void cleanup() {
        info("ServerThread cleanup() start");
        try {
            // close server-side end of connection
            client.close();
            info("Closed Server-side Socket");
        } catch (IOException e) {
            info("Client already closed");
        }
        info("ServerThread cleanup() end");
    }
}