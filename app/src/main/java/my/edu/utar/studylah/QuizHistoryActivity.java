package my.edu.utar.studylah;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.util.List;

import my.edu.utar.studylah.room.AppDatabase;
import my.edu.utar.studylah.room.QuestionEntity;
import my.edu.utar.studylah.room.QuizSessionWithQuestions;

public class QuizHistoryActivity extends AppCompatActivity {

    private LinearLayout layoutTitles;
    private LinearLayout layoutDetails;
    private ScrollView layoutDetailsScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_history);

        layoutTitles = findViewById(R.id.layoutTitles);
        layoutDetails = findViewById(R.id.layoutDetails);
        layoutDetailsScroll = findViewById(R.id.layoutDetailsScroll);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(QuizHistoryActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });


        loadQuizTitles();
    }
    private void loadQuizTitles() {
        new Thread(() -> {
            my.edu.utar.studylah.room.AppDatabase db = my.edu.utar.studylah.room.AppDatabase.getInstance(getApplicationContext());
            List<String> allTitles = db.quizDao().getAllTitles();

            runOnUiThread(() -> {
                layoutTitles.removeAllViews();

                for (int i = 0; i < allTitles.size(); i++) {
                    String rawTitle = allTitles.get(i);
                    String displayTitle = (rawTitle == null || rawTitle.trim().isEmpty()) ? "Untitled Quiz" : rawTitle;

                    View titleRow = getLayoutInflater().inflate(R.layout.item_quiz_title, layoutTitles, false);
                    TextView tvTitle = titleRow.findViewById(R.id.tvQuizTitle);
                    TextView tvNumber = titleRow.findViewById(R.id.tvNumber);
                    ImageButton btnDelete = titleRow.findViewById(R.id.btnDelete);
                    TextView tvDate = titleRow.findViewById(R.id.tvQuizDate);

                    tvNumber.setText((i + 1) + ".");
                    tvTitle.setText(displayTitle);


                    // Fetch and show recent date
                    new Thread(() -> {
                        String latestDate = db.quizDao().getMostRecentDateForTitle(rawTitle);
                        runOnUiThread(() -> {
                            if (latestDate != null) {
                                tvDate.setText("🕒 " + latestDate);
                            } else {
                                tvDate.setText("No attempt yet");
                            }
                        });
                    }).start();

                    tvTitle.setOnClickListener(v -> {
                        layoutTitles.setVisibility(View.GONE);
                        layoutDetails.setVisibility(View.VISIBLE);
                        layoutDetailsScroll.setVisibility(View.VISIBLE);
                        showSessionsForTitle(rawTitle);
                    });

                    btnDelete.setOnClickListener(v -> {
                        new AlertDialog.Builder(this)
                                .setTitle("Delete Quiz History")
                                .setMessage("Are you sure you want to delete \"" + displayTitle + "\"?")
                                .setPositiveButton("Delete", (dialog, which) -> {
                                    new Thread(() -> {
                                        db.quizDao().deleteQuestionsByTitle(rawTitle);
                                        db.quizDao().deleteSessionsByTitle(rawTitle);

                                        runOnUiThread(() -> {
                                            layoutTitles.removeView(titleRow);
                                            layoutDetails.removeAllViews();
                                            layoutDetails.setVisibility(View.GONE);
                                            layoutDetailsScroll.setVisibility(View.GONE);
                                            layoutTitles.setVisibility(View.VISIBLE);
                                            Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                        });
                                    }).start();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });

                    layoutTitles.addView(titleRow);
                }
            });
        }).start();
    }

    private void showSessionsForTitle(String title) {
        layoutDetails.removeAllViews();

        Button backButton = new Button(this);
        backButton.setText("BACK TO QUIZ LISTS");
        backButton.setTextColor(Color.WHITE);
        backButton.setBackgroundColor(Color.parseColor("#94b1ff"));
        backButton.setTextSize(16f);
        backButton.setTypeface(ResourcesCompat.getFont(this, R.font.lexend_regular));
        backButton.setPadding(30, 20, 30, 20);

        backButton.setOnClickListener(v -> {
            layoutDetailsScroll.setVisibility(View.GONE);
            layoutDetails.setVisibility(View.GONE);
            layoutTitles.setVisibility(View.VISIBLE);
        });
        layoutDetails.addView(backButton);

        new Thread(() -> {
            my.edu.utar.studylah.room.AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<QuizSessionWithQuestions> sessions = db.quizDao().getSessionsByTitle(title);

            runOnUiThread(() -> {
                if (sessions.isEmpty()) {
                    TextView noData = new TextView(this);
                    noData.setText("No sessions found.");
                    layoutDetails.addView(noData);
                    return;
                }

                for (QuizSessionWithQuestions record : sessions) {
                    TextView sessionDate = new TextView(this);
                    sessionDate.setText("🕒 " + record.session.date +
                            " | Score: " + record.session.score + "/" + record.session.totalQuestions);
                    sessionDate.setPadding(0, 20, 0, 15);
                    sessionDate.setTextSize(14); // Increase text size (in sp)

                    // Apply custom font
                    Typeface lexend = ResourcesCompat.getFont(this, R.font.lexend_regular);
                    sessionDate.setTypeface(lexend);

                    layoutDetails.addView(sessionDate);

                    for (int i = 0; i < record.questions.size(); i++) {
                        QuestionEntity q = record.questions.get(i);
                        View qCard = getLayoutInflater().inflate(R.layout.item_quiz_question, layoutDetails, false);

                        TextView tvQuestionNumber = qCard.findViewById(R.id.tvQuestionNumber);
                        TextView tvOptions = qCard.findViewById(R.id.tvOptions);
                        TextView tvCorrectAnswer = qCard.findViewById(R.id.tvCorrectAnswer);
                        TextView tvSelectedAnswer = qCard.findViewById(R.id.tvSelectedAnswer);

                        tvQuestionNumber.setText((i + 1) + ". " + q.question);
                        tvOptions.setText("A. " + q.optionA + "\nB. " + q.optionB + "\nC. " + q.optionC + "\nD. " + q.optionD);
                        tvCorrectAnswer.setText("✔ Correct Answer: " + q.correctAnswer);

                        tvSelectedAnswer.setText("🙋 Selected Answer: " + q.selectedAnswer);
                        if (q.selectedAnswer != null && q.selectedAnswer.equalsIgnoreCase(q.correctAnswer)) {
                            tvSelectedAnswer.setTextColor(Color.parseColor("#666666")); // gray for correct
                        } else {
                            tvSelectedAnswer.setTextColor(Color.parseColor("#FF4444")); // red for incorrect
                        }
                        layoutDetails.addView(qCard);
                    }
                }
            });
        }).start();
    }

}
