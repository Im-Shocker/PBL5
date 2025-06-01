package com.example.pbl5;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TCPClient extends Thread {
    private String serverIP = "192.168.233.116";
    private int serverPort = 12345;
    private Handler handler;
    private Socket socket;
    private BufferedReader input;
    private boolean running = true;

    public TCPClient(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(serverIP, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Kết nối thành công -> gửi thông báo lên UI
            Message successMsg = handler.obtainMessage(1, "Đã kết nối tới server");
            handler.sendMessage(successMsg);

            String message;
            while (running && (message = input.readLine()) != null) {
                Message msg = handler.obtainMessage(0, message);
                handler.sendMessage(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();

            // Kết nối thất bại -> gửi thông báo lỗi lên UI
            Message failMsg = handler.obtainMessage(2, "Không thể kết nối tới server: " + e.getMessage());
            Log.d("Loi", e.getMessage());
            handler.sendMessage(failMsg);
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
