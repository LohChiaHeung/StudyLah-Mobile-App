package my.edu.utar.studylah;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;


import java.util.ArrayList;
import java.util.List;

public class ShelfActivity extends AppCompatActivity {
    private AppDatabase db;
    private FolderAdapter folderAdapter;
    private RecyclerView recyclerView;
    private TextView textPath;
    private Integer currentFolderId = null;
    private final List<Integer> folderHistory = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelf);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "study_db").allowMainThreadQueries().build();


        recyclerView = findViewById(R.id.recyclerView);
        textPath = findViewById(R.id.textPath);

        folderAdapter = new FolderAdapter(
                new ArrayList<>(),
                new ArrayList<>(),
                this::onFolderClick,
                this::onPdfClick
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(folderAdapter);

        loadContents();

        findViewById(R.id.btnCreateFolder).setOnClickListener(v -> createFolder());
        findViewById(R.id.btnImportPdf).setOnClickListener(v -> importPdf());
    }

    // In ShelfActivity.java, update the onFolderClick method
    private void onFolderClick(FolderEntity folder) {
        folderHistory.add(currentFolderId);  // ✅ store null if you're in root
        currentFolderId = folder.id;

        textPath.setText("Folder: " + folder.folderName);
        loadContents();
    }

    private void loadContents() {
        List<FolderEntity> folders = db.folderDao().getChildFolders(currentFolderId);
        List<PdfEntity> pdfs = db.pdfDao().getChildPdfs(currentFolderId);
        folderAdapter.updateData(folders, pdfs);
    }

    private void createFolder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Folder Name");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString();
            if (!name.isEmpty()) {
                db.folderDao().insertFolder(new FolderEntity(name, currentFolderId));
                loadContents();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void importPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, 200);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            int folderId = data.getIntExtra("current_folder_id", -1);
            if (folderId != -1) {
                currentFolderId = folderId;
                textPath.setText(currentFolderId == null ? "Root Folder" : "Folder: " + getFolderName(folderId));
                loadContents();
            }
            return;
        }

        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();

            int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri, takeFlags);

            String name = getFileName(uri);
            PdfEntity pdf = new PdfEntity(name, uri.toString(), currentFolderId);
            db.pdfDao().insertPdf(pdf);
            loadContents();
        }


    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(nameIndex);
                }
            } finally {
                cursor.close();
            }
        }
        return result != null ? result : uri.getLastPathSegment();
    }

    // view
    private void onPdfClick(PdfEntity pdf) {
        // ✅ Add this debug log FIRST
        Log.d("ShelfActivity", "Opening PDF URI: " + pdf.getUri());

        Intent intent = new Intent(this, PdfViewerActivity.class);
        intent.putExtra("pdf_uri", pdf.getUri());
        intent.putExtra("current_folder_id", currentFolderId);
        startActivityForResult(intent, 101); // ✅ receive result

    }

    public void deleteFolder(FolderEntity folder) {
        db.pdfDao().deletePdfsInFolder(folder.id);
        db.folderDao().deleteFolderById(folder.id);
        loadContents();
    }

    public void deletePdf(PdfEntity pdf) {
        db.pdfDao().deletePdfById(pdf.pdf_id);
        loadContents();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return handleBackNavigation();
    }

    private boolean handleBackNavigation() {
        Log.d("ShelfActivity", "Handling back navigation. History size: " + folderHistory.size());

        if (!folderHistory.isEmpty()) {
            // Go back one folder level
            currentFolderId = folderHistory.remove(folderHistory.size() - 1);

            String folderName = (currentFolderId == null)
                    ? "Shelf"
                    : getFolderName(currentFolderId);

            Log.d("ShelfActivity", "Navigating back to folder: " + folderName + " (ID: " + currentFolderId + ")");
            if (folderName == "Shelf"){
                textPath.setText(folderName);
            }else{
                textPath.setText("Folder: " + folderName);
            }
            loadContents();
            return true;
        } else {
            Log.d("ShelfActivity", "No more folder history, returning to MainActivity");
            finish();
            return false;
        }
    }


    private String getFolderName(int folderId) {
        try {
            FolderEntity folder = db.folderDao().getFolderById(folderId);
            return folder != null ? folder.folderName : "Unknown Folder";
        } catch (Exception e) {
            Log.e("ShelfActivity", "Error getting folder name: " + e.getMessage());
            return "Unknown Folder";
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_folder_id", currentFolderId != null ? currentFolderId : -1);
        // Save the folder history as an ArrayList of Integers
        outState.putIntegerArrayList("folder_history", new ArrayList<>(folderHistory));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            int folderId = savedInstanceState.getInt("current_folder_id", -1);
            if (folderId != -1) {
                currentFolderId = folderId;
            } else {
                currentFolderId = null;
            }

            ArrayList<Integer> history = savedInstanceState.getIntegerArrayList("folder_history");
            if (history != null) {
                folderHistory.clear();
                folderHistory.addAll(history);
            }

            // Update the UI based on restored state
            if (currentFolderId != null) {
                textPath.setText("Folder: " + getFolderName(currentFolderId));
            } else {
                textPath.setText("Root Folder");
            }
            loadContents();
        }
    }



}
