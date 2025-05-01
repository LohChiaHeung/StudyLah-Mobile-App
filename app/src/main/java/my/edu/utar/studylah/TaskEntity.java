package my.edu.utar.studylah;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskEntity {

    @PrimaryKey(autoGenerate = true)
    public int taskId;

    @ColumnInfo(name = "taskName")
    public String taskName;

    @ColumnInfo(name = "pdfUri")
    public String pdfUri;

    @ColumnInfo(name = "folderId")
    public int folderId;

    @ColumnInfo(name = "dueDate")
    public String dueDate;

    @ColumnInfo(name = "isCompleted")
    public boolean isCompleted;

    @ColumnInfo(name = "taskType") // NEW
    public String taskType; // "auto" or "manual"

    public TaskEntity(String taskName, String pdfUri, int folderId, String dueDate, boolean isCompleted) {
        this.taskName = taskName;
        this.pdfUri = pdfUri;
        this.folderId = folderId;
        this.dueDate = dueDate;
        this.isCompleted = isCompleted;
        this.taskType = "manual";
    }
}