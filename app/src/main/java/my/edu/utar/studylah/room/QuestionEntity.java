package my.edu.utar.studylah.room;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "questions",
        foreignKeys = @ForeignKey(
                entity = QuizSessionEntity.class,
                parentColumns = "sessionId",
                childColumns = "sessionOwnerId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("sessionOwnerId")}
)

public class QuestionEntity {
    @PrimaryKey(autoGenerate = true)
    public int questionId;

    public int sessionOwnerId;

    public String question;
    public String optionA;
    public String optionB;
    public String optionC;
    public String optionD;
    public String correctAnswer;
    public String selectedAnswer;
}

