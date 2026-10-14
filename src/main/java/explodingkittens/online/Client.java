package explodingkittens.online;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Client for connecting to an Exploding Kittens server over network sockets.
 */
public class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean running;

    public Client() {
        this.running = false;
    }

    /**
     * Connects to the server at the given IP and port, and starts listening for server messages.
     *
     * @param ip   server IP address or hostname
     * @param port server port
     * @throws IOException if network connection fails
     */
    public void connect(String ip, int port) throws IOException {
        this.socket = new Socket(ip, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        this.running = true;

        Thread listenerThread = new Thread(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (running && socket != null && !socket.isClosed()) {
                try {
                    Object msg = in.readObject();
                    if (msg != null) {
                        String text = msg.toString();
                        System.out.print(text);
                        if (!text.endsWith("\n") && !text.endsWith(" ")) {
                            System.out.println();
                        }

                        boolean isNopePrompt = text.contains("<Enter>");
                        boolean isActionPrompt = text.contains("options")
                                || text.contains("Choose an action")
                                || text.contains("Select")
                                || text.contains("Where in the deck")
                                || text.contains("Give a card")
                                || text.contains("[PROMPT]")
                                || text.endsWith(": ")
                                || text.endsWith("? ");

                        if (isNopePrompt) {
                            int waited = 0;
                            while (!br.ready() && waited < 5000) {
                                Thread.sleep(200);
                                waited += 200;
                            }
                            if (br.ready()) {
                                String nopeResponse = br.readLine();
                                sendCommand(nopeResponse);
                            } else {
                                sendCommand(" ");
                            }
                        } else if (isActionPrompt) {
                            String input = br.readLine();
                            if (input != null) {
                                sendCommand(input);
                            }
                        }
                    }
                } catch (Exception e) {
                    running = false;
                    System.out.println("\nDisconnected from server.");
                    break;
                }
            }
        });
        listenerThread.start();
    }

    /**
     * Sends a command or action string to the server.
     *
     * @param cmd command string
     */
    public synchronized void sendCommand(String cmd) {
        if (out != null && running) {
            try {
                out.writeObject(cmd);
                out.flush();
            } catch (IOException e) {
                running = false;
            }
        }
    }

    /**
     * Disconnects from the server and closes resources.
     */
    public void disconnect() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public Socket getSocket() {
        return socket;
    }

    public ObjectOutputStream getOut() {
        return out;
    }

    public ObjectInputStream getIn() {
        return in;
    }

    public boolean isRunning() {
        return running;
    }
}
