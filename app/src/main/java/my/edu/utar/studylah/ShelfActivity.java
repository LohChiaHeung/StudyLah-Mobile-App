package my.edu.utar.studylah;

import android.annotation.SuppressLint;
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
import androidx.appcompat.widget.SearchView;

import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;


import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private androidx.appcompat.widget.SearchView searchView;
    private List<PdfEntity> allPdfs = new ArrayList<>(); // To store all PDFs from current and subfolders
    private List<FolderEntity> currentFolders = new ArrayList<>();
    private RecyclerView searchResultRecyclerView;
    private SearchResultAdapter searchResultAdapter;
    private static final String TAG = "ShelfActivity"; // For logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelf);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Set up back press handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "study_db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();

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

        findViewById(R.id.btnImportPdf).setOnClickListener(v -> {
            showBottomSheetOptions();
        });


        findViewById(R.id.btnGenerateTasks).setOnClickListener(v -> showTaskGenerationDialog());
        findViewById(R.id.btnViewTasks).setOnClickListener(v -> {
            Intent intent = new Intent(ShelfActivity.this, TaskListActivity.class);
            intent.putExtra("folder_id", -1); // Pass -1 to load all tasks
            startActivity(intent);
        });

        searchResultRecyclerView = findViewById(R.id.searchResultRecyclerView);
        searchResultRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultAdapter = new SearchResultAdapter(new ArrayList<>(), this::onPdfClick);
        searchResultRecyclerView.setAdapter(searchResultAdapter);
        searchView = findViewById(R.id.searchView);

        loadContents();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterPdf(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterPdf(newText);
                return true;
            }
        });

    }

    // Fixed onFolderClick method to prevent duplicate history entries
    private void onFolderClick(FolderEntity folder) {
        Log.d(TAG, "onFolderClick: Before - currentFolderId=" + currentFolderId + ", history=" + folderHistory);

        // Only add to history if current folder is not null and not already the last item in history
        if (currentFolderId != null &&
                (folderHistory.isEmpty() || !currentFolderId.equals(folderHistory.get(folderHistory.size() - 1)))) {
            folderHistory.add(currentFolderId);
        }

        currentFolderId = folder.id;
        updatePathText();
        loadContents();

        Log.d(TAG, "onFolderClick: After - currentFolderId=" + currentFolderId + ", history=" + folderHistory);
    }

    private void loadContents() {
        Log.d(TAG, "loadContents: Loading for folderId=" + currentFolderId + ", history=" + folderHistory);
        currentFolders = db.folderDao().getChildFolders(currentFolderId);
        List<PdfEntity> directPdfs = db.pdfDao().getChildPdfs(currentFolderId);

        allPdfs = getAllPdfsRecursive(currentFolderId);
        folderAdapter.updateData(currentFolders, directPdfs);
    }

    private List<PdfEntity> getAllPdfsRecursive(Integer parentId) {
        List<PdfEntity> result = new ArrayList<>(db.pdfDao().getChildPdfs(parentId));
        List<FolderEntity> subFolders = db.folderDao().getChildFolders(parentId);
        for (FolderEntity folder : subFolders) {
            result.addAll(getAllPdfsRecursive(folder.id));
        }
        return result;
    }

    private void createFolder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Folder Name");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                db.folderDao().insertFolder(new FolderEntity(name, currentFolderId));
                loadContents();
            } else {
                Toast.makeText(this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
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

    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            ArrayList<Integer> returnedHistory = data.getIntegerArrayListExtra("folder_history");
            int folderId = data.getIntExtra("current_folder_id", -1);

            Log.d(TAG, "onActivityResult: Returned from PDF: folderId=" + folderId + ", history=" + returnedHistory);

            if (returnedHistory != null) {
                // Clean up the history to avoid duplicates
                folderHistory.clear();
                for (Integer id : returnedHistory) {
                    // Avoid consecutive duplicates in history
                    if (folderHistory.isEmpty() || !id.equals(folderHistory.get(folderHistory.size() - 1))) {
                        folderHistory.add(id);
                    }
                }
            }

            currentFolderId = (folderId != -1) ? folderId : null;
            updatePathText();
            loadContents();
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

    private void filterPdf(String query) {
        if (query.trim().isEmpty()) {
            searchResultRecyclerView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE); // ✅ show main content
            return;
        }

        // Filter PDF names only (not folders)
        List<PdfEntity> filtered = new ArrayList<>();
        for (PdfEntity pdf : allPdfs) {
            if (pdf.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(pdf);
            }
        }

        // Show only results in list view
        searchResultAdapter.updateResults(filtered);
        searchResultRecyclerView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE); // ✅ hide main folders/pdfs
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
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return result != null ? result : uri.getLastPathSegment();
    }

    // Fixed onPdfClick to simplify history handling
    private void onPdfClick(PdfEntity pdf) {
        Log.d(TAG, "onPdfClick: Opening PDF, current history=" + folderHistory);

        // Simply launch the PDF viewer with current state
        Intent intent = new Intent(this, PdfViewerActivity.class);
        intent.putExtra("pdf_uri", pdf.getUri());
        intent.putExtra("pdf_name", pdf.getName());
        intent.putExtra("current_folder_id", pdf.getParentFolderId());
        intent.putIntegerArrayListExtra("folder_history", new ArrayList<>(folderHistory));
        startActivityForResult(intent, 101);
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

    // Improved back navigation handling
    private boolean handleBackNavigation() {
        Log.d(TAG, "handleBackNavigation: History before=" + folderHistory + ", currentFolderId=" + currentFolderId);

        if (!folderHistory.isEmpty()) {
            // Remove any potential duplicates of current folder from the end of history
            while (!folderHistory.isEmpty() &&
                    currentFolderId != null &&
                    currentFolderId.equals(folderHistory.get(folderHistory.size() - 1))) {
                folderHistory.remove(folderHistory.size() - 1);
                Log.d(TAG, "handleBackNavigation: Removed duplicate from history");
            }

            if (!folderHistory.isEmpty()) {
                // Go back one folder level
                currentFolderId = folderHistory.remove(folderHistory.size() - 1);

                Log.d(TAG, "handleBackNavigation: New currentFolderId=" + currentFolderId +
                        ", Updated history=" + folderHistory);

                updatePathText();
                loadContents();
                return true;
            }
        }

        // If we're already at the root level (currentFolderId is null), finish the activity
        // Otherwise, go back to the root level first
        if (currentFolderId != null) {
            // Go back to root level
            currentFolderId = null;
            folderHistory.clear();
            updatePathText();
            loadContents();
            return true;
        }

        // Only finish the activity if we're already at the root level
        Log.d(TAG, "handleBackNavigation: No more history and at root level, finishing activity");
        finish();
        return true;
    }

    private String getFolderName(int folderId) {
        try {
            FolderEntity folder = db.folderDao().getFolderById(folderId);
            return folder != null ? folder.folderName : "Unknown Folder";
        } catch (Exception e) {
            Log.e(TAG, "Error getting folder name: " + e.getMessage());
            return "Unknown Folder";
        }
    }

    private void updatePathText() {
        List<String> pathParts = new ArrayList<>();
        if (currentFolderId == null) {
            pathParts.add("Shelf");
        } else {
            pathParts.add("Shelf");
            Integer tempId = currentFolderId;
            while (tempId != null) {
                FolderEntity folder = db.folderDao().getFolderById(tempId);
                if (folder != null) {
                    pathParts.add(1, folder.folderName); // Insert after Shelf
                    tempId = folder.parentFolderId;
                } else {
                    break;
                }
            }
        }
        textPath.setText(String.join(" > ", pathParts));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_folder_id", currentFolderId != null ? currentFolderId : -1);
        outState.putIntegerArrayList("folder_history", new ArrayList<>(folderHistory));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            int folderId = savedInstanceState.getInt("current_folder_id", -1);
            currentFolderId = (folderId != -1) ? folderId : null;

            ArrayList<Integer> history = savedInstanceState.getIntegerArrayList("folder_history");
            if (history != null) {
                folderHistory.clear();
                folderHistory.addAll(history);
            }

            updatePathText();
            loadContents();
        }
    }

    private void showBottomSheetOptions() {
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_options, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        TextView createFolder = view.findViewById(R.id.option_create_folder);
        TextView importPdf = view.findViewById(R.id.option_import_pdf);

        createFolder.setOnClickListener(v -> {
            dialog.dismiss();
            createFolder();
        });

        importPdf.setOnClickListener(v -> {
            dialog.dismiss();
            importPdf();
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (searchView.getQuery().toString().trim().isEmpty()) {
            searchResultRecyclerView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
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
                    String inputText = input.getText().toString();
                    if (!inputText.isEmpty()) {
                        int days = Integer.parseInt(inputText);
                        generateTasks(selectedPdfs, days);
                    } else {
                        Toast.makeText(this, "Please enter a valid number of days", Toast.LENGTH_SHORT).show();
                    }
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