package my.edu.utar.studylah;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskEntity {
    @PrimaryKey(autoGenerate = true)
    public int taskId;

    public String taskName;
    public String pdfUri;
    public int folderId;
    public String dueDate;

    public boolean isCompleted;

    public TaskEntity(String taskName, String pdfUri, int folderId, String dueDate, boolean isCompleted) {
        this.taskName = taskName;
        this.pdfUri = pdfUri;
        this.folderId = folderId;
        this.dueDate = dueDate;
        this.isCompleted = isCompleted;
    }
}