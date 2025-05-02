package my.edu.utar.studylah;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import my.edu.utar.studylah.room.AppDatabase;
import my.edu.utar.studylah.room.QuestionEntity;
import my.edu.utar.studylah.room.QuizDao;
import my.edu.utar.studylah.room.QuizSessionEntity;

public class QuizActivity extends AppCompatActivity {

    private List<QuestionModel> questions;
    private String quizDate;
    private int currentIndex = 0;
    private int score = 0;

    private TextView txtQuestion, txtCorrectAnswers;
    private Button btnA, btnB, btnC, btnD, btnSubmit, btnReset;
    private String selectedAnswer = "";

    private Drawable defaultBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        txtQuestion = findViewById(R.id.txtQuestion);
        btnA = findViewById(R.id.btnA);
        btnB = findViewById(R.id.btnB);
        btnC = findViewById(R.id.btnC);
        btnD = findViewById(R.id.btnD);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnReset = findViewById(R.id.btnReset);
        txtCorrectAnswers = findViewById(R.id.txtCorrectAnswers);
        ImageButton btnBack = findViewById(R.id.btnBack);


        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(QuizActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // Optional: finish QuizActivity to prevent back navigation loop
        });

        defaultBackground = btnA.getBackground();

        questions = loadQuestions();
        if (questions == null) questions = new ArrayList<>();

        txtCorrectAnswers.setText("Correct Answers: 0/" + questions.size());

        if (!questions.isEmpty()) {
            showQuestion(currentIndex);
        } else {
            Toast.makeText(this, "No questions found.", Toast.LENGTH_LONG).show();
        }

        quizDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        btnA.setOnClickListener(v -> selectAnswer("A"));
        btnB.setOnClickListener(v -> selectAnswer("B"));
        btnC.setOnClickListener(v -> selectAnswer("C"));
        btnD.setOnClickListener(v -> selectAnswer("D"));

        btnSubmit.setOnClickListener(v -> {
            if (selectedAnswer.isEmpty()) {
                Toast.makeText(this, "Please select an answer first.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedAnswer.equals(questions.get(currentIndex).getCorrectAnswer())) {
                score++;
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show();
            }

            questions.get(currentIndex).setSelectedAnswer(selectedAnswer);
            txtCorrectAnswers.setText("Correct Answers: " + score + "/" + questions.size());

            if (currentIndex < questions.size() - 1) {
                currentIndex++;
                selectedAnswer = "";
                showQuestion(currentIndex);
            } else {
                saveToRoom();
                showScore();
            }
        });

        btnReset.setOnClickListener(v -> clearSelection());

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void selectAnswer(String option) {
        selectedAnswer = option;
        clearHighlight();
        getButtonByOption(option).setBackgroundResource(R.drawable.rec_test_grey);
    }


    private void clearHighlight() {
        btnA.setBackground(defaultBackground);
        btnB.setBackground(defaultBackground);
        btnC.setBackground(defaultBackground);
        btnD.setBackground(defaultBackground);
    }

    private Button getButtonByOption(String option) {
        switch (option) {
            case "A": return btnA;
            case "B": return btnB;
            case "C": return btnC;
            case "D": return btnD;
            default: return null;
        }
    }

    private void clearSelection() {
        selectedAnswer = "";
        clearHighlight();
    }

    private String capitalizeSentence(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    private void showQuestion(int index) {
        QuestionModel q = questions.get(index);
        txtQuestion.setText("Question " + (index + 1) + ": " + q.getQuestion());
        btnA.setText("A. " + capitalizeSentence(q.getOptions()[0]));
        btnB.setText("B. " + capitalizeSentence(q.getOptions()[1]));
        btnC.setText("C. " + capitalizeSentence(q.getOptions()[2]));
        btnD.setText("D. " + capitalizeSentence(q.getOptions()[3]));
        clearSelection();
    }

    private void showScore() {
        View view = getLayoutInflater().inflate(R.layout.dialog_quiz_result, null);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = view.findViewById(R.id.tvDialogMessage);

        tvMessage.setText("You got " + score + " out of " + questions.size());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("View History", (dialogInterface, which) -> {
                    startActivity(new Intent(this, QuizHistoryActivity.class));
                    finish();
                })
                .create();

        dialog.show();

        // Customize button after showing
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(Color.parseColor("#2196F3")); // Blue color
            positiveButton.setTextSize(18); // Font size
            positiveButton.setTypeface(ResourcesCompat.getFont(this, R.font.lexend_regular));
        }
    }

    private List<QuestionModel> loadQuestions() {
        String json = getIntent().getStringExtra("generated_questions");
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<QuestionModel>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }

    private void saveToRoom() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            QuizDao dao = db.quizDao();

            QuizSessionEntity session = new QuizSessionEntity();
            String pdfTitle = getIntent().getStringExtra("pdf_title");
            if (pdfTitle == null || pdfTitle.trim().isEmpty()) {
                pdfTitle = "Untitled Quiz";
            }
            session.title = pdfTitle;
            session.date = quizDate;
            session.score = score;
            session.totalQuestions = questions.size();

            long sessionId = dao.insertSession(session);

            List<QuestionEntity> entityList = new ArrayList<>();
            for (QuestionModel q : questions) {
                QuestionEntity qe = new QuestionEntity();
                qe.sessionOwnerId = (int) sessionId;
                qe.question = q.getQuestion();
                qe.optionA = q.getOptions()[0];
                qe.optionB = q.getOptions()[1];
                qe.optionC = q.getOptions()[2];
                qe.optionD = q.getOptions()[3];
                qe.correctAnswer = q.getCorrectAnswer();
                qe.selectedAnswer = q.getSelectedAnswer();
                entityList.add(qe);
            }
            dao.insertQuestions(entityList);
        }).start();
    }
}
