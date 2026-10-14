package explodingkittens.online;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Handles network IO communication for an online client connection.
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean running;
    private final BlockingQueue<String> messageQueue;

    /**
     * Constructs a ClientHandler wrapping the given socket.
     *
     * @param socket the client socket
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.running = true;
        this.messageQueue = new LinkedBlockingQueue<>();
        if (socket != null && !socket.isClosed()) {
            try {
                this.out = new ObjectOutputStream(socket.getOutputStream());
                this.out.flush();
                this.in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                this.running = false;
            }
        }
    }

    /**
     * Listens for incoming messages from the client in a background thread.
     */
    public void listen() {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        while (running && socket != null && !socket.isClosed()) {
            try {
                Object received = in.readObject();
                if (received != null) {
                    String msg = received.toString();
                    messageQueue.offer(msg);
                    onMessageReceived(msg);
                }
            } catch (Exception e) {
                running = false;
                break;
            }
        }
    }

    /**
     * Hook called when a message is received.
     *
     * @param message the received message
     */
    protected void onMessageReceived(String message) {
        // Can be overridden or extended by listeners
    }

    /**
     * Sends a message to the connected client.
     *
     * @param msg the message to send
     */
    public synchronized void sendMessage(String msg) {
        if (out != null && running) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                running = false;
            }
        }
    }

    /**
     * Reads a message from the client, waiting up to 60 seconds if necessary.
     *
     * @return the message read, or an empty string on timeout/error
     */
    public String readMessage() {
        try {
            String msg = messageQueue.poll(60, TimeUnit.SECONDS);
            return msg != null ? msg : "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    /**
     * Closes the socket and streams.
     */
    public void close() {
        running = false;
        messageQueue.offer("");
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isRunning() {
        return running;
    }
}
