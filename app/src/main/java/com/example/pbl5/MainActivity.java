package com.example.pbl5;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.pbl5.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SpeechReceiver";
    private static final int PORT = 6000;

    // View Binding
    private ActivityMainBinding binding;

    // Data và logic
    private ArrayList<String> history = new ArrayList<>();
    private HistoryAdapter historyAdapter;
    private TextToSpeech tts;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private boolean isReceiving = false;
    private boolean isServerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeViews();
        setupTextToSpeech();
        setupToggleButton();
        startServer();
    }

    private void initializeViews() {
        // Setup ListView với custom adapter đẹp
        historyAdapter = new HistoryAdapter(this, history);
        binding.lvHistory.setAdapter(historyAdapter);

        // Thiết lập trạng thái ban đầu
        updateToggleButton();
        updateHistoryCount();
        showEmptyState();

        Log.i(TAG, "Views đã được khởi tạo với giao diện đẹp");
    }

    private void setupTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("vi", "VN"));
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Không hỗ trợ tiếng Việt, chuyển sang tiếng Anh");
                    tts.setLanguage(Locale.US);
                }
                Log.i(TAG, "TextToSpeech đã sẵn sàng");
                updateStatusMessage("🎤 Hệ thống phát âm đã sẵn sàng", false);
            } else {
                Log.e(TAG, "Khởi tạo TextToSpeech thất bại");
                showToast("Không thể khởi tạo chức năng đọc văn bản");
                updateStatusMessage("❌ Lỗi khởi tạo hệ thống phát âm", true);
            }
        });
    }

    private void setupToggleButton() {
        binding.btnOnOff.setOnClickListener(v -> {
            // Animation cho button
            v.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

            isReceiving = !isReceiving;
            updateToggleButton();

            String status = isReceiving ? "BẬT" : "TẮT";
            showToast("Đã " + status + " chế độ nhận cử chỉ");

            // Cập nhật status message
            if (isReceiving) {
                updateStatusMessage("✅ Đang lắng nghe cử chỉ từ Raspberry Pi", false);
            } else {
                updateStatusMessage("⏸️ Tạm dừng nhận cử chỉ", false);
            }

            Log.i(TAG, "Chế độ nhận cử chỉ: " + status);
        });
    }

    private void updateToggleButton() {
        if (isReceiving) {
            binding.btnOnOff.setText("TẮT Nhận Cử Chỉ");
            binding.btnOnOff.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_stop));
            binding.btnOnOff.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.button_secondary));
        } else {
            binding.btnOnOff.setText("BẬT Nhận Cử Chỉ");
            binding.btnOnOff.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_play));
            binding.btnOnOff.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.button_primary));
        }

        // Animation cho transition
        binding.btnOnOff.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    }

    private void updateStatusMessage(String message, boolean isError) {
        binding.tvCuChi.setText(message);

        // Thay đổi background dựa vào trạng thái
        if (isError) {
            binding.tvCuChi.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.status_error));
        } else {
            binding.tvCuChi.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.status_success));
        }

        // Animation cho status update
        binding.tvCuChi.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
    }

    private void updateHistoryCount() {
        binding.tvHistoryCount.setText(String.valueOf(history.size()));

        // Show/hide empty state
        if (history.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState() {
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.lvHistory.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        binding.emptyState.setVisibility(View.GONE);
        binding.lvHistory.setVisibility(View.VISIBLE);
    }

    private void startServer() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isServerRunning = true;
                Log.i(TAG, "Server đang lắng nghe trên port " + PORT);

                runOnUiThread(() -> {
                    showToast("🚀 Server đã khởi động trên port " + PORT);
                    updateStatusMessage("🌐 Server đang chạy - Port: " + PORT + "\n📱 Chờ kết nối từ Raspberry Pi...", false);
                });

                while (isServerRunning && !Thread.currentThread().isInterrupted()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        Log.i(TAG, "Có kết nối từ: " + clientSocket.getInetAddress());

                        // Hiển thị thông tin kết nối với animation
                        runOnUiThread(() -> {
                            String connectionInfo = "🔗 Đã kết nối với: " + clientSocket.getInetAddress() + "\n⏳ Đang chờ dữ liệu...";
                            updateStatusMessage(connectionInfo, false);
                        });

                        // Xử lý từng kết nối trong thread riêng
                        handleClientConnection(clientSocket);

                    } catch (IOException e) {
                        if (isServerRunning) {
                            Log.e(TAG, "Lỗi khi chấp nhận kết nối: " + e.getMessage());
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Lỗi khởi tạo server: " + e.getMessage());
                runOnUiThread(() -> {
                    showToast("❌ Không thể khởi động server");
                    updateStatusMessage("❌ Lỗi server: " + e.getMessage(), true);
                });
            }
        });
    }

    private void handleClientConnection(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()))) {

            String message;
            while ((message = in.readLine()) != null) {
                final String finalMessage = message.trim();

                if (!finalMessage.isEmpty()) {
                    Log.i(TAG, "Nhận được tin nhắn: " + finalMessage);

                    runOnUiThread(() -> {
                        if (isReceiving) {
                            processReceivedMessage(finalMessage);
                        } else {
                            Log.i(TAG, "Tin nhắn bị bỏ qua (chế độ tắt): " + finalMessage);
                            // Vẫn hiển thị tin nhắn nhưng không đọc to
                            updateStatusMessage("⏸️ (Tạm dừng) " + finalMessage, false);
                        }
                    });
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Lỗi đọc dữ liệu từ client: " + e.getMessage());
            runOnUiThread(() -> {
                updateStatusMessage("⚠️ Mất kết nối với client", true);
            });
        } finally {
            try {
                clientSocket.close();
                Log.i(TAG, "Đã đóng kết nối client");
                runOnUiThread(() -> {
                    updateStatusMessage("📱 Chờ kết nối mới từ Raspberry Pi...", false);
                });
            } catch (IOException e) {
                Log.e(TAG, "Lỗi đóng kết nối client: " + e.getMessage());
            }
        }
    }

    private void processReceivedMessage(String message) {
        // Cập nhật UI với animation
        updateStatusMessage("🎯 " + message, false);

        // Thêm vào lịch sử với emoji và timestamp
        String timestampedMessage = getCurrentTimestamp() + " 🔸 " + message;
        history.add(0, timestampedMessage);

        // Giới hạn lịch sử (giữ tối đa 50 tin nhắn)
        if (history.size() > 50) {
            history.remove(history.size() - 1);
        }

        historyAdapter.notifyDataSetChanged();
        updateHistoryCount();

        // Auto scroll to top để xem tin nhắn mới nhất
        binding.lvHistory.smoothScrollToPosition(0);

        // Animation cho list view
        binding.lvHistory.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        // Đọc tin nhắn
        speakMessage(message);

        // Hiển thị toast với emoji
        showToast("📢 " + message);

        Log.i(TAG, "Đã xử lý tin nhắn: " + message);
    }

    private void speakMessage(String message) {
        if (tts != null) {
            // Thêm pause ngắn trước khi đọc
            String speakText = message;
            tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "message_id");
            Log.d(TAG, "🔊 Đang đọc: " + message);

            // Cập nhật UI để hiển thị đang đọc
            runOnUiThread(() -> {
                updateStatusMessage("🔊 Đang phát: " + message, false);
            });
        } else {
            Log.w(TAG, "TTS chưa sẵn sàng");
            runOnUiThread(() -> {
                updateStatusMessage("⚠️ Hệ thống phát âm chưa sẵn sàng", true);
            });
        }
    }

    private String getCurrentTimestamp() {
        return new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new java.util.Date());
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Dừng server
        isServerRunning = false;

        // Đóng TextToSpeech
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            Log.i(TAG, "TTS đã được dọn dẹp");
        }

        // Đóng ServerSocket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                Log.i(TAG, "Server socket đã đóng");
            }
        } catch (IOException e) {
            Log.e(TAG, "Lỗi đóng server socket: " + e.getMessage());
        }

        // Dừng ExecutorService
        if (executorService != null) {
            executorService.shutdown();
            Log.i(TAG, "ExecutorService đã dừng");
        }

        // Dọn dẹp binding
        binding = null;

        Log.i(TAG, "🧹 Ứng dụng đã được dọn dẹp hoàn toàn");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Tạm dừng TTS khi ứng dụng không active
        if (tts != null) {
            tts.stop();
            Log.d(TAG, "TTS đã tạm dừng");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 Ứng dụng đã resume");

        // Cập nhật UI khi quay lại app
        if (isServerRunning) {
            updateStatusMessage("🔄 Ứng dụng đã hoạt động trở lại", false);
        }
    }
}