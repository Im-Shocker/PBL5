package com.example.pbl5;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextToSpeech tts;
    private TextView tvShow, tvConnect, tvWord;
    private Button btnPower;
    private boolean listening = false;
    private DatabaseReference dataRef;
    private DatabaseReference dataWord;
    private String currentWord = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        tvShow = findViewById(R.id.tvShow);
        tvWord = findViewById(R.id.tvWord);
        tvConnect = findViewById(R.id.tvConnect);
        btnPower = findViewById(R.id.btnPower);
        FirebaseApp.initializeApp(this);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.forLanguageTag("vi-VN"));
            }
        });

        dataRef = FirebaseDatabase.getInstance().getReference("raspberry_pi/text");
        dataWord = FirebaseDatabase.getInstance().getReference("raspberry_pi/word");

        btnPower.setOnClickListener(v -> {
            if (!listening) {
                startListening();
            } else {
                stopListening();
            }
        });
    }
    private final ValueEventListener wordListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            String word = snapshot.getValue(String.class);
            if (word != null && !word.isEmpty()) {
                tvWord.setText("Từ: " + word);
                speak(word);
            }
        }
        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.e("Firebase", "Error reading word: " + error.getMessage());
        }
    };
    private void startListening() {
        listening = true;
        tvConnect.setText("Đang dịch ...");
        btnPower.setText("Tắt");
        dataRef.addValueEventListener(firebaseListener);
        dataWord.addValueEventListener(wordListener);
    }

    private void stopListening() {
        listening = false;
        tvConnect.setText("Đã ngắt kết nối");
        btnPower.setText("Bật");
        tvShow.setText("Ký hiệu: ???");
        dataRef.removeEventListener(firebaseListener);
    }

    private final ValueEventListener firebaseListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            String value = snapshot.getValue(String.class);
            if (value != null) {
                tvShow.setText("Ký hiệu: " + value);
                currentWord = value;
                if(!currentWord.equals("0"))
                    speak(value);
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Toast.makeText(MainActivity.this, "Lỗi đọc Firebase", Toast.LENGTH_SHORT).show();
            Log.e("Firebase", "Error: " + error.getMessage());
        }
    };

    private void speak(String text) {
        if (tts != null && !tts.isSpeaking()) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "UTTERANCE_ID");
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        stopListening();
        super.onDestroy();
    }
}
