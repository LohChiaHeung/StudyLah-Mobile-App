package my.edu.utar.studylah.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quiz_sessions")
public class QuizSessionEntity {
    @PrimaryKey(autoGenerate = true)
    public int sessionId;
    public int score;
    public int totalQuestions;

    public String title;     // PDF title or folder name
    public String date;      // Timestamp
}


