package my.edu.utar.studylah;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import my.edu.utar.studylah.MainActivity;
import my.edu.utar.studylah.R;

public class LoadingActivity extends AppCompatActivity {

    private static final int LOADING_DURATION = 3000;
    private static final long CHAR_DELAY = 200;

    private TextView loadingText;
    private String fullText = "LOADING...";
    private int charIndex = 0;
    private Handler textHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        loadingText = findViewById(R.id.loadingText);
        loadingText.setText("");

        startTypingAnimation();

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(LoadingActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, LOADING_DURATION);
    }

    private void startTypingAnimation() {
        textHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (charIndex < fullText.length()) {
                    loadingText.append(String.valueOf(fullText.charAt(charIndex)));
                    charIndex++;
                    textHandler.postDelayed(this, CHAR_DELAY);
                }
            }
        }, CHAR_DELAY);
    }
}
