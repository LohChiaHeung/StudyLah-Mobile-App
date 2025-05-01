package my.edu.utar.studylah;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;


//Testing
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

        Button btnViewTasks = findViewById(R.id.btnViewTasks);
        btnViewTasks.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TaskListActivity.class);
            startActivity(intent);
        });


        Button btnGenerateQuiz = findViewById(R.id.btnGenerateQuiz);
        btnGenerateQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            pdfLauncher.launch(Intent.createChooser(intent, "Select PDF"));
        });


        Button btnQuizHistory = findViewById(R.id.btnQuizHistory);
        btnQuizHistory.setOnClickListener(v -> {
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
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
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