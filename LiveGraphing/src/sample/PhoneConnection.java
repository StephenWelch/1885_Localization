package sample;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Stephen Welch on 10/25/2017.
 */
public class PhoneConnection extends Thread{

    private AdbBridge adb;
    private ServerSocket m_server_socket = null;
    private Socket m_socket = null;

    private Grapher grapher;

    public PhoneConnection(Grapher grapher) {
        this.grapher = grapher;
    }

    @Override
    public void run() {
        while (m_socket == null) {
            attemptConnection();
        }

        try {
            System.out.println("Intializing data stream.");
            InputStream is = m_socket.getInputStream();
            byte[] buffer = new byte[2048];
            int read;
            while (m_socket.isConnected() && (read = is.read(buffer)) != -1) {
                String messageRaw = new String(buffer, 0, read);
                String[] messages = messageRaw.split("\n");
                for (String message : messages) {
                    System.out.printf("%s\n", message);
                    int xIndex = message.indexOf('x');
                    int yIndex = message.indexOf('y');
                    float x = Float.valueOf(message.substring(xIndex + 1, yIndex));
                    float y = Float.valueOf(message.substring(yIndex + 1, message.length()));
                    System.out.println("X: " + x + " Y: " + y);
                    grapher.addDataPoint(x, y);
                }
            }
            System.out.println("Socket disconnected.");
        } catch (IOException e) {
            System.err.println("Could not talk to socket.");
        }
        if (m_socket != null) {
            try {
                System.out.println("Stopping");
                adb.stop();
                m_server_socket.close();
                m_socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

        private void attemptConnection() {
            try {
                System.out.println("Waiting for connection.");
                m_server_socket = new ServerSocket(1885);
                adb = new AdbBridge();
                adb.start();
                adb.reversePortForward(1885, 1885);
                m_socket = m_server_socket.accept();
            } catch (IOException e) {
                System.err.println("Error accepting connection.");
                m_socket = null;
                e.printStackTrace();
            }
        }
}

