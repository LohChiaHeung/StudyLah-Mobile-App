package my.edu.utar.studylah;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "folders")
public class FolderEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "folder_name")
    public String folderName;

    @ColumnInfo(name = "parent_folder_id")
    public Integer parentFolderId;

    // Constructor
    public FolderEntity(String folderName, Integer parentFolderId) {
        this.folderName = folderName;
        this.parentFolderId = parentFolderId;
    }

    // Getters
    public String getFolderName() {
        return folderName;
    }

    public Integer getParentFolderId() {
        return parentFolderId;
    }

    public int getId() {
        return id;
    }

}


