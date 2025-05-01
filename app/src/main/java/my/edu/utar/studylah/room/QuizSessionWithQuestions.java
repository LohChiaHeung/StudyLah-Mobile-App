package my.edu.utar.studylah.room;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class QuizSessionWithQuestions {
    @Embedded
    public QuizSessionEntity session;

    @Relation(
            parentColumn = "sessionId",
            entityColumn = "sessionOwnerId"
    )
    public List<QuestionEntity> questions;
}

