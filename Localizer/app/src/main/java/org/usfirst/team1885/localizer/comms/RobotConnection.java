package org.usfirst.team1885.localizer.comms;

import android.content.Context;
import android.content.Intent;
import android.util.Log;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RobotConnection {
    public static final String TAG = RobotConnection.class.getSimpleName();
    public static final int K_ROBOT_PORT = 1885;
    public static final String K_ROBOT_PROXY_HOST = "localhost";
    public static final int K_CONNECTOR_SLEEP_MS = 100;

    private int m_port;
    private String m_host;
    private Context m_context;
    private boolean m_running = true;
    private boolean m_connected = false;
    volatile private Socket m_socket;
    private Thread m_connect_thread, m_write_thread;
    private ArrayBlockingQueue<String> mToSend = new ArrayBlockingQueue<String>(30);

    protected class WriteThread implements Runnable {

        @Override
        public void run() {
            while (m_running) {
                String nextToSend = null;
                try {
                    nextToSend = mToSend.poll(250, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Log.e("WriteThead", "Couldn't poll queue");
                }
                if (nextToSend == null) {
                    continue;
                }
                sendToWire(nextToSend);
            }
        }
    }

    protected class ConnectionMonitor implements Runnable {

        @Override
        public void run() {
            while (m_running) {
                try {
                    if (m_socket == null || !m_socket.isConnected() && !m_connected) {
                        tryConnect();
                        Thread.sleep(250, 0);
                    }
                    Thread.sleep(K_CONNECTOR_SLEEP_MS, 0);
                } catch (InterruptedException e) {
                }
            }

        }
    }

    public RobotConnection(Context context, String host, int port) {
        m_context = context;
        m_host = host;
        m_port = port;
    }

    public RobotConnection(Context context) {
        this(context, K_ROBOT_PROXY_HOST, K_ROBOT_PORT);
    }

    synchronized private void tryConnect() {
        if (m_socket == null) {
            try {
                m_socket = new Socket(m_host, m_port);
               // m_socket.setSoTimeout(100);
            } catch (IOException e) {
                Log.w("RobotConnector", "Could not connect");
                m_socket = null;
            }
        }
    }

    synchronized public void stop() {
        m_running = false;
        if (m_connect_thread != null && m_connect_thread.isAlive()) {
            try {
                m_connect_thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (m_write_thread != null && m_write_thread.isAlive()) {
            try {
                m_write_thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    synchronized public void start() {
        m_running = true;

        if (m_write_thread == null || !m_write_thread.isAlive()) {
            m_write_thread = new Thread(new WriteThread());
            m_write_thread.start();
        }

        if (m_connect_thread == null || !m_connect_thread.isAlive()) {
            m_connect_thread = new Thread(new ConnectionMonitor());
            m_connect_thread.start();
        }
    }


    synchronized public void restart() {
        stop();
        start();
    }

    synchronized public boolean isConnected() {
        return m_socket != null && m_socket.isConnected() && m_connected;
    }

    private synchronized boolean sendToWire(String message) {
        String toSend = message + "\n";
        if (m_socket != null && m_socket.isConnected()) {
            try {
                OutputStream os = m_socket.getOutputStream();
                os.write(toSend.getBytes());
                return true;
            } catch (IOException e) {
                Log.w("RobotConnection", "Could not send data to socket, try to reconnect");
                m_socket = null;
            }
        }
        return false;
    }

    public synchronized boolean send(String message) {
        return mToSend.offer(message);
    }

}
