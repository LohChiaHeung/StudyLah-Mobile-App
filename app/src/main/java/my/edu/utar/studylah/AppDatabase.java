package my.edu.utar.studylah;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {FolderEntity.class, PdfEntity.class, TaskEntity.class}, version = 5) // ✅ UPDATED HERE
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract FolderDao folderDao();
    public abstract PdfDao pdfDao();
    public abstract TaskDao taskDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "study_db")
                            .fallbackToDestructiveMigration() // ✅ important if you don't use full migration yet
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
