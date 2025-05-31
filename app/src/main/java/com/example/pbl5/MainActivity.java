package com.example.pbl5;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView txtResult;
    private Button btnDetect, btnToggle;

    private boolean isDetecting = true;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapping();
        startCamera();

        btnDetect.setOnClickListener(v -> {
            if (!isDetecting) {
                Toast.makeText(MainActivity.this, "Hệ thống đang tắt nhận diện", Toast.LENGTH_SHORT).show();
                return;
            }
            // Giả lập nhận diện ký hiệu
            String fakeGesture = "A";
            txtResult.setText("Ký hiệu: " + fakeGesture);
        });

        btnToggle.setOnClickListener(v -> {
            isDetecting = !isDetecting;
            String status = isDetecting ? "Bật nhận diện" : "Tắt nhận diện";
            Toast.makeText(MainActivity.this, status, Toast.LENGTH_SHORT).show();
        });
    }

    private void mapping() {
        previewView = findViewById(R.id.cam_nhan_dien);
        txtResult = findViewById(R.id.tv_ky_hieu);
        btnDetect = findViewById(R.id.btn_nhan_dien);
        btnToggle = findViewById(R.id.btn_bat_tat);
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();



                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
