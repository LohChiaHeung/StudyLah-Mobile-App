package my.edu.utar.studylah;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

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

    }
    // test push 1
}