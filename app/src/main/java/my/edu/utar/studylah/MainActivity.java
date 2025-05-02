package my.edu.utar.studylah;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> pdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    showQuizTitleDialog(result.getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Open Shelf Card
        MaterialCardView cardShelf = findViewById(R.id.cardOpenShelf);
        cardShelf.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ShelfActivity.class);
            startActivity(intent);
        });

        // Pomodoro Timer Card
        MaterialCardView cardPomodoro = findViewById(R.id.cardPomodoro);
        cardPomodoro.setOnClickListener(v -> {
            Intent intent2 = new Intent(MainActivity.this, PomodoroActivity.class);
            startActivity(intent2);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.statusbar_color));
        });

        // AI Chatbot Button click listener
        FloatingActionButton btnChatbot = findViewById(R.id.btnChatbot);
        btnChatbot.setOnClickListener(v -> {
            // Navigate to AiChatbotPage
            Intent intent = new Intent(MainActivity.this, AiChatbotPage.class);
            startActivity(intent);
        });


        // View Tasks Card
        MaterialCardView cardViewTasks = findViewById(R.id.cardViewTasks);
        cardViewTasks.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TaskListActivity.class);
            startActivity(intent);
        });

        // Generate Quiz Card
        MaterialCardView cardGenerateQuiz = findViewById(R.id.cardGenerateQuiz);
        cardGenerateQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            pdfLauncher.launch(Intent.createChooser(intent, "Select PDF"));
        });

        // Quiz History Card
        MaterialCardView cardQuizHistory = findViewById(R.id.cardQuizHistory);
        cardQuizHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, QuizHistoryActivity.class);
            startActivity(intent);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.statusbar_color));
        });
    }

    private void showQuizTitleDialog(Intent data) {
        // Use LinearLayout to wrap and pad the EditText
        LinearLayout container = new LinearLayout(this);
        container.setPadding(60, 20, 60, 0); // add equal left and right padding
        container.setOrientation(LinearLayout.VERTICAL);

        EditText input = new EditText(this);
        input.setHint("Enter Quiz Title");
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        input.setTextSize(16f);
        input.setTypeface(ResourcesCompat.getFont(this, R.font.lexend_regular));

        // Match parent so it aligns with buttons inside the dialog
        input.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("📝 Name Your Quiz")
                .setView(container)
                .setPositiveButton("Start", (dialogInterface, which) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) title = "Untitled Quiz";

                    Intent intent = new Intent(this, MCQGeneratorActivity.class);
                    intent.putExtra("pdf_uri", data.getData().toString());
                    intent.putExtra("quiz_title", title);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Style the dialog buttons
        android.widget.Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        android.widget.Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Typeface lexend = ResourcesCompat.getFont(this, R.font.lexend_regular);
        if (positive != null) {
            positive.setTypeface(lexend);
            positive.setTextColor(Color.parseColor("#2196F3"));
        }
        if (negative != null) {
            negative.setTypeface(lexend);
            negative.setTextColor(Color.DKGRAY);
        }
    }
}