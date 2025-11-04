package clarkson.ee408.tictactoev4.client;

import clarkson.ee408.tictactoev4.socket.Request;
import clarkson.ee408.tictactoev4.socket.Response;
import com.google.gson.Gson;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A Singleton class that manages the client-side socket connection,
 * ensuring only one connection is established across the application.
 */
public class SocketClient {

    // ------------------- Static Variable (Singleton Instance) -------------------

    /** Stores the single instance of the SocketClient class. */
    private static SocketClient instance;

    // ------------------- Class Attributes -------------------

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Gson gson; // Used for serialization and deserialization

    // Executor for handling network operations off the main UI thread
    private ExecutorService executor;

    // ------------------- Host and Port Configuration -------------------

    // NOTE: This must match the server configuration
    private static final String HOST = "10.0.2.2"; // Android emulator loopback address for host machine
    private static final int PORT = 5000;

    // ------------------- Private Constructor -------------------

    /**
     * Private constructor for the Singleton pattern.
     * Initializes Gson and the ExecutorService.
     */
    private SocketClient() {
        this.gson = new Gson();
        // Use a single-thread executor for synchronous network requests
        this.executor = Executors.newSingleThreadExecutor();
    }

    // ------------------- Static Getter (Thread-Safe) -------------------

    /**
     * Provides the global access point to the single SocketClient instance.
     * Uses double-checked locking for thread-safe lazy initialization.
     *
     * @return The single SocketClient instance.
     */
    public static SocketClient getInstance() {
        if (instance == null) {
            synchronized (SocketClient.class) { // Synchronized block for thread safety
                if (instance == null) {
                    instance = new SocketClient();
                }
            }
        }
        return instance;
    }

    // ------------------- Connection Management -------------------

    /**
     * Connects to the server and initializes I/O streams.
     * Note: This method should be called on a separate thread (e.g., using the Executor).
     *
     * @throws IOException if connection or stream setup fails.
     */
    public void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(HOST, PORT);
            outputStream = new DataOutputStream(socket.getOutputStream());
            inputStream = new DataInputStream(socket.getInputStream());
        }
    }

    /**
     * Closes the socket connection and all I/O streams.
     */
    public void close() {
        try {
            if (outputStream != null) outputStream.close();
        } catch (IOException e) {
            System.err.println("Error closing output stream: " + e.getMessage());
        }
        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {
            System.err.println("Error closing input stream: " + e.getMessage());
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }

    // ------------------- Request/Response Handling (Generic) -------------------

    /**
     * Sends a serialized Request to the server and waits for a response.
     * This function should be called ONLY from the background executor thread.
     *
     * @param request The Request object to be sent.
     * @param responseClass The Class/Type of the expected Response (e.g., Response.class or GamingResponse.class).
     * @param <T> The generic type of the expected Response class.
     * @return The deserialized response object of type T, or null on failure.
     */
    public <T extends Response> T sendRequest(Request request, Class<T> responseClass) {
        if (socket == null || socket.isClosed()) {
            System.err.println("Socket is not connected. Cannot send request.");
            return null;
        }

        try {
            // 1. Serialize the request
            String serializedRequest = gson.toJson(request);

            // 2. Send the serialized request
            outputStream.writeUTF(serializedRequest);
            outputStream.flush();

            // 3. Read the serialized response from the server
            String serializedResponse = inputStream.readUTF();

            // 4. Deserialize the response to the specified class/type
            return gson.fromJson(serializedResponse, (Type) responseClass);

        } catch (IOException e) {
            System.err.println("Error during socket communication: " + e.getMessage());
            // Attempt to close the connection on communication failure
            close();
            return null;
        } catch (Exception e) {
            System.err.println("Error during serialization/deserialization: " + e.getMessage());
            return null;
        }
    }

    // ------------------- Executor Getter -------------------

    /**
     * Getter for the ExecutorService to allow the MainActivity to run network tasks.
     * @return The ExecutorService.
     */
    public ExecutorService getExecutor() {
        return executor;
    }
}