package clarkson.ee408.tictactoev4.client;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import clarkson.ee408.tictactoev4.socket.Request;
import clarkson.ee408.tictactoev4.socket.Response;

/**
 * A singleton class that helps the Android application connect to the socket server.
 */
public final class SocketClient {

    private static final Object CONNECTION_LOCK_OBJECT = new Object();
    private static final Object LOCK_OBJECT = new Object();
    private static final String SERVER_HOST = "10.128.27.197";
    private static final int SERVER_PORT = 5000;
    private static final int SOCKET_TIMEOUT = 10000; // 10 seconds
    private static final String TAG = "SocketClient";

    private static SocketClient INSTANCE;

    private final Gson gson;

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private SocketClient() {
        this.gson = new GsonBuilder().serializeNulls().create();
    }

    /**
     * Close the socket connection and all I/O streams.
     */
    public void close() {
        synchronized (CONNECTION_LOCK_OBJECT) {
            quietClose(this.inputStream);
            quietClose(this.outputStream);
            quietClose(this.socket);
            Log.i(TAG, "Connection closed");
        }
    }

    /**
     * Sends a {@code Request} to the server serialized as JSON and returns the response deserialized as {@code responseClass}.
     *
     * @param request the object to serialize and send to the server
     * @param responseClass the class of T
     * @param <T> the type of the desired object
     * @return the response from the server deserialized from JSON to an object of class T. Returns
     *      {@code null} if the response is null, empty, or invalid JSON
     * @throws IOException on errors connecting or communicating with the server
     */
    public <T extends Response> T sendRequest(Request request, Class<T> responseClass)
            throws IOException {

        synchronized (CONNECTION_LOCK_OBJECT) {
            // Open the connection to the server
            connect();

            // Serialize the request to JSON and send it to the server
            String requestJson = this.gson.toJson(request);
            this.outputStream.writeUTF(requestJson);
            this.outputStream.flush();

            // Wait for a JSON response
            String response = this.inputStream.readUTF();

            // Deserialize the received JSON
            try {
                return this.gson.fromJson(response, responseClass);
            } catch (JsonSyntaxException ex) {
                Log.e(TAG, "Error deserializing JSON", ex);
                return null;
            }
        }
    }

    /**
     * Returns the only instance of {@code SocketClient}.
     *
     * @return the {@code SocketClient} instance
     */
    public static SocketClient getInstance() {
        if (INSTANCE == null) {
            synchronized (LOCK_OBJECT) {
                if (INSTANCE == null) {
                    INSTANCE = new SocketClient();
                }
            }
        }

        return INSTANCE;
    }

    private void quietClose(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ex) {
            // nom-nom the exception
        }
    }

    private void connect() throws IOException {
        // Synchronization is handled by the caller
        if (this.socket == null || !this.socket.isConnected()) {
            // Create socket and connect
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));

            Log.i(TAG, "Client connected to server");

            // Don't want to wait forever for input from the server
            this.socket.setSoTimeout(SOCKET_TIMEOUT);

            // Set up streams
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
        }
    }
}