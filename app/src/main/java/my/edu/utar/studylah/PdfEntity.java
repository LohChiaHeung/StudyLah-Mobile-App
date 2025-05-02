package my.edu.utar.studylah;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pdfs")
public class PdfEntity {
    @PrimaryKey(autoGenerate = true)
    public int pdf_id;

    @ColumnInfo(name = "pdf_name")
    public String pdf_name;

    @ColumnInfo(name = "pdf_uri")
    public String pdf_uri;

    @ColumnInfo(name = "parent_folder_id")
    public Integer parentFolderId;

    public PdfEntity(String pdf_name, String pdf_uri, Integer parentFolderId) {
        this.pdf_name = pdf_name;
        this.pdf_uri = pdf_uri;
        this.parentFolderId = parentFolderId;
    }

    public void setName(String pdf_name) {
        this.pdf_name = pdf_name;
    }
    public String getName() {
        return pdf_name;
    }

    public String getUri() {
        return pdf_uri;
    }

    public Integer getParentFolderId() {
        return parentFolderId;
    }
}



