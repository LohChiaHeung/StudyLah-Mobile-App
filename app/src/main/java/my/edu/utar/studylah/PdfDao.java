package my.edu.utar.studylah;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PdfDao {

    @Insert
    void insertPdf(PdfEntity pdf);

    @Query("SELECT * FROM pdfs WHERE parent_folder_id IS :parentId")
    List<PdfEntity> getChildPdfs(Integer parentId);

    @Query("DELETE FROM pdfs WHERE parent_folder_id = :folderId")
    void deletePdfsInFolder(int folderId);

    @Query("DELETE FROM pdfs WHERE pdf_id = :pdfId")
    void deletePdfById(int pdfId);

    @Query("SELECT * FROM pdfs WHERE parent_folder_id IS NULL")
    List<PdfEntity> getRootPdfs();
}



