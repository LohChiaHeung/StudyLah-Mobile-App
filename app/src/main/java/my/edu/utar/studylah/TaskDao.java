package my.edu.utar.studylah;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {
    @Insert
    void insertTask(TaskEntity task);

    @Query("SELECT * FROM tasks")
    List<TaskEntity> getAllTasks();

    @Query("SELECT * FROM tasks WHERE folderId = :folderId")
    List<TaskEntity> getTasksForFolder(int folderId);

    @Query("UPDATE tasks SET isCompleted = 1 WHERE taskId = :taskId")
    void markTaskComplete(int taskId);

    @Query("DELETE FROM tasks WHERE taskId = :taskId")
    void deleteTask(int taskId);

    @Update
    void updateTask(TaskEntity task);
}