package my.edu.utar.studylah.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface QuizDao {
    @Insert
    long insertSession(QuizSessionEntity session); // Returns inserted sessionId

    @Insert
    void insertQuestions(List<QuestionEntity> questions);

    @Transaction
    @Query("SELECT * FROM quiz_sessions ORDER BY date DESC")
    List<QuizSessionWithQuestions> getAllSessionsWithQuestions();


    @Transaction
    @Query("SELECT * FROM quiz_sessions WHERE COALESCE(title, 'Untitled Quiz') = :title")
    List<QuizSessionWithQuestions> getSessionsByTitle(String title);


    @Query("SELECT DISTINCT IFNULL(title, 'Untitled Quiz') FROM quiz_sessions ORDER BY date DESC")
    List<String> getAllTitles();

    @Query("DELETE FROM questions WHERE sessionOwnerId IN (SELECT sessionId FROM quiz_sessions WHERE title = :title)")
    void deleteQuestionsByTitle(String title);

    @Query("DELETE FROM quiz_sessions WHERE title = :title")
    void deleteSessionsByTitle(String title);

    @Query("SELECT date FROM quiz_sessions WHERE title = :title ORDER BY sessionId DESC LIMIT 1")
    String getMostRecentDateForTitle(String title);
}
