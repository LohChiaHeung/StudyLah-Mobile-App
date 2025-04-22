package my.edu.utar.studylah;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        findViewById(R.id.btnGenerateTasks).setOnClickListener(v -> showTaskGenerationDialog());

        findViewById(R.id.btnViewTasks).setOnClickListener(v -> {
            Intent intent = new Intent(ShelfActivity.this, TaskListActivity.class);
            intent.putExtra("folder_id", -1); // Pass -1 to load all tasks
            startActivity(intent);
        });
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

    private void showTaskGenerationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.generate_task_dialog, null);
        ExpandableListView expandableListView = dialogView.findViewById(R.id.expandableListView);

        List<FolderEntity> folders = db.folderDao().getRootFolders();
        Map<String, List<PdfEntity>> folderMap = new HashMap<>();
        Map<String, List<Boolean>> selectionMap = new HashMap<>();

        for (FolderEntity folder : folders) {
            List<PdfEntity> pdfs = db.pdfDao().getChildPdfs(folder.id);
            folderMap.put(folder.folderName, pdfs);
            List<Boolean> selected = new ArrayList<>();
            for (int i = 0; i < pdfs.size(); i++) selected.add(false);
            selectionMap.put(folder.folderName, selected);
        }

        List<String> folderNames = new ArrayList<>(folderMap.keySet());

        ExpandableListAdapter adapter = new BaseExpandableListAdapter() {
            @Override public int getGroupCount() { return folderNames.size(); }
            @Override public int getChildrenCount(int group) { return folderMap.get(folderNames.get(group)).size(); }
            @Override public Object getGroup(int group) { return folderNames.get(group); }
            @Override public Object getChild(int group, int child) { return folderMap.get(folderNames.get(group)).get(child); }
            @Override public long getGroupId(int group) { return group; }
            @Override public long getChildId(int group, int child) { return child; }
            @Override public boolean hasStableIds() { return false; }

            @Override
            public View getGroupView(int group, boolean isExpanded, View convertView, ViewGroup parent) {
                TextView tv = new TextView(ShelfActivity.this);
                tv.setPadding(80, 16, 16, 16);
                tv.setTextSize(17f);
                tv.setText(folderNames.get(group));
                return tv;
            }

            @Override
            public View getChildView(int group, int child, boolean isLast, View convertView, ViewGroup parent) {
                CheckBox cb = new CheckBox(ShelfActivity.this);
                String folder = folderNames.get(group);
                PdfEntity pdf = folderMap.get(folder).get(child);
                cb.setText(pdf.getName());
                cb.setChecked(selectionMap.get(folder).get(child));
                cb.setOnCheckedChangeListener((buttonView, isChecked) ->
                        selectionMap.get(folder).set(child, isChecked));
                return cb;
            }

            @Override
            public boolean isChildSelectable(int group, int child) { return true; }
        };

        expandableListView.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setTitle("Select PDFs to generate tasks")
                .setView(dialogView)
                .setPositiveButton("Next", (dialog, which) -> {
                    List<PdfEntity> selectedPdfs = new ArrayList<>();
                    for (String folder : folderMap.keySet()) {
                        List<PdfEntity> pdfs = folderMap.get(folder);
                        List<Boolean> flags = selectionMap.get(folder);
                        for (int i = 0; i < pdfs.size(); i++) {
                            if (flags.get(i)) selectedPdfs.add(pdfs.get(i));
                        }
                    }
                    if (selectedPdfs.isEmpty()) {
                        Toast.makeText(this, "Please select at least one PDF", Toast.LENGTH_SHORT).show();
                    } else {
                        showDurationDialog(selectedPdfs);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDurationDialog(List<PdfEntity> selectedPdfs) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter number of days");

        new AlertDialog.Builder(this)
                .setTitle("Set Duration")
                .setView(input)
                .setPositiveButton("Generate Tasks", (dialog, which) -> {
                    int days = Integer.parseInt(input.getText().toString());
                    generateTasks(selectedPdfs, days);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void generateTasks(List<PdfEntity> selectedPdfs, int days) {
        TaskDao taskDao = db.taskDao();

        long currentTime = System.currentTimeMillis();
        long oneDayMillis = 24 * 60 * 60 * 1000L;

        int totalPdfs = selectedPdfs.size();
        int perDay = (int) Math.ceil((double) totalPdfs / days);

        int pdfIndex = 0;

        for (int day = 0; day < days; day++) {
            String dueDate = new java.text.SimpleDateFormat("yyyy-MM-dd")
                    .format(new java.util.Date(currentTime + (day * oneDayMillis)));

            for (int j = 0; j < perDay && pdfIndex < totalPdfs; j++, pdfIndex++) {
                PdfEntity pdf = selectedPdfs.get(pdfIndex);

                String taskName = "Read: " + pdf.getName();
                TaskEntity task = new TaskEntity(taskName, pdf.getUri(), -1, dueDate, false);
                taskDao.insertTask(task);
            }
        }

        Toast.makeText(this, "Smart tasks generated over " + days + " days!", Toast.LENGTH_SHORT).show();
    }
}
