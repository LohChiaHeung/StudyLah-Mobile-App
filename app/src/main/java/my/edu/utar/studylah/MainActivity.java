package my.edu.utar.studylah;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

//Testing
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnShelf = findViewById(R.id.btnOpenShelf);
        btnShelf.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ShelfActivity.class);
            startActivity(intent);
        });

        Button btnPomodoro = findViewById(R.id.btnPomodoro);
        btnPomodoro.setOnClickListener(v -> {
            Intent intent2 = new Intent(MainActivity.this, PomodoroActivity.class);
            startActivity(intent2);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.statusbar_color));
        });

        Button btnchatbot = findViewById(R.id.btnChatbot);
        btnchatbot.setOnClickListener(v -> {
            Intent intent3 = new Intent(MainActivity.this, AiChatbotPage.class);
            startActivity(intent3);
        });

    }

}