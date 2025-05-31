package com.example.pbl5;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextToSpeech tts;
    private TCPClient tcpClient;
    private Handler handler;
    private TextView tvShow, tvConnect;
    private Button btnPower;
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvShow = findViewById(R.id.tvShow);
        tvConnect = findViewById(R.id.tvConnect);
        btnPower = findViewById(R.id.btnPower);
        handler = new Handler(message -> {
            switch (message.what) {
                case 0: // Nhận dữ liệu từ server
                    String data = (String) message.obj;
                    // Hiển thị dữ liệu lên TextView
                    tvShow.setText("Dữ liệu từ server: " + data);
                    speak(data);
                    break;
                case 1: // Kết nối thành công
                    Toast.makeText(this, (String) message.obj, Toast.LENGTH_SHORT).show();
                    tvConnect.setText("Trạng thái: Đã kết nối");
                    break;
                case 2: // Kết nối thất bại
                    Toast.makeText(this, (String) message.obj, Toast.LENGTH_SHORT).show();
                    tvConnect.setText("Trạng thái: Không thể kết nối");
                    break;
            }
            return true;
        });

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.forLanguageTag("vi-VN"));
            }
        });
        btnPower.setOnClickListener(v -> {
            if (!connected) {
                startConnection();
            } else {
                stopConnection();
            }
        });
    }
    private void speak(String text) {
        if (tts != null && !tts.isSpeaking()) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "UTTERANCE_ID");
        }
    }
    @Override
    protected void onDestroy() {
        connected = false;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
    private void startConnection() {
        tcpClient = new TCPClient(handler);
        tcpClient.start();
        connected = true;
        btnPower.setText("Tắt");
        tvConnect.setText("Đang kết nối...");
    }

    private void stopConnection() {
        if (tcpClient != null) {
            tcpClient.close();
            tcpClient = null;
        }
        connected = false;
        btnPower.setText("Bật");
        tvConnect.setText("Trạng thái: ???");
        tvShow.setText("Ký hiệu: Chưa xác định");
    }
}

