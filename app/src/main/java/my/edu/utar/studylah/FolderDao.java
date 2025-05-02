package my.edu.utar.studylah;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FolderDao {

    @Insert
    void insertFolder(FolderEntity folder);

    @Query("SELECT * FROM folders WHERE parent_folder_id IS :parentId")
    List<FolderEntity> getChildFolders(Integer parentId);

    @Query("SELECT * FROM folders WHERE parent_folder_id IS NULL")
    List<FolderEntity> getRootFolders();

    /* 🔹 NEW: fetch one folder by its PK */
    @Query("SELECT * FROM folders WHERE id = :folderId LIMIT 1")
    FolderEntity getFolderById(int folderId);

    @Query("SELECT * FROM pdfs WHERE pdf_name LIKE '%' || :query || '%'")
    List<PdfEntity> searchPdfs(String query);


    @Query("DELETE FROM folders WHERE id = :folderId")
    void deleteFolderById(int folderId);

    @Update
    void updateFolder(FolderEntity folder);



}