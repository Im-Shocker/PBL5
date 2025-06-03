package com.example.pbl5;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient extends Thread {
    private String serverIp;
    private int serverPort = 1234;
    private Handler handler;
    private Socket socket;
    private BufferedReader input;
    private boolean running = true;

    public TCPClient(Handler handler, String serverIp) {
        this.handler = handler;
        this.serverIp = serverIp;
    }

    @Override
    public void run() {
        try {
            InetAddress serverAddress = InetAddress.getByName(serverIp);
            socket = new Socket(serverAddress, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Message successMsg = handler.obtainMessage(1, "Đã kết nối tới server");
            handler.sendMessage(successMsg);

            String message;
            while (running && (message = input.readLine()) != null) {
                Message msg = handler.obtainMessage(0, message);
                handler.sendMessage(msg);
            }
        } catch (IOException e) {
            Message failMsg = handler.obtainMessage(2, "Không thể kết nối tới server: " + e.getMessage());
            handler.sendMessage(failMsg);
            Log.e("TCPClient", "Lỗi kết nối: " + e.getMessage());
        } finally {
            close();
        }
    }

    public void close() {
        running = false;
        try {
            if (input != null) input.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
