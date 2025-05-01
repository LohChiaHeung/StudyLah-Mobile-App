package my.edu.utar.studylah;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskListActivity extends AppCompatActivity {

    private AppDatabase db;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<TaskEntity> taskList;
    private TaskDao taskDao;
    private TextView textTotalTasks, textCompletedTasks, textTodayTask;
    private ImageButton btnBack;
    private Button btnAddTask;
    private String selectedDate; // ✅ Track currently selected date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        db = AppDatabase.getInstance(this);
        taskDao = db.taskDao();

        btnBack = findViewById(R.id.btnBack);
        btnAddTask = findViewById(R.id.btnAddTask);
        textTotalTasks = findViewById(R.id.textTotalTasks);
        textCompletedTasks = findViewById(R.id.textCompletedTasks);
        textTodayTask = findViewById(R.id.textTodayTask);
        recyclerView = findViewById(R.id.taskRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        selectedDate = getTodayDate(); // default today
        loadTasksForDate(selectedDate);

        adapter = new TaskAdapter(this, taskList, taskDao);
        recyclerView.setAdapter(adapter);

        setupBackButton();
        setupAddTaskButton();
        setupSwipeToDelete();
        setupTotalTasksClick();
        updateTaskOverview();
        updateTodayDate(selectedDate);

        textTodayTask.setOnClickListener(v -> showDatePickerDialog()); // ✅ Make title clickable
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupAddTaskButton() {
        btnAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    private void setupTotalTasksClick() {
        View.OnClickListener goToAllTasks = v -> {
            Intent intent = new Intent(TaskListActivity.this, AllTasksActivity.class);
            startActivity(intent);
        };

        textTotalTasks.setOnClickListener(goToAllTasks);
        textCompletedTasks.setOnClickListener(goToAllTasks);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                TaskEntity task = adapter.getTaskAt(position);

                adapter.removeTask(position);
                taskDao.deleteTask(task.taskId);

                Snackbar.make(recyclerView, "Task deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v -> {
                            adapter.addTask(position, task);
                            taskDao.insertTask(task);
                            reloadTasksAfterCheckboxChange();
                        }).show();

                reloadTasksAfterCheckboxChange();
            }
        };

        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(recyclerView);
    }

    public void reloadTasksAfterCheckboxChange() {
        loadTasksForDate(selectedDate); // reload based on current viewing date
        adapter.setTaskList(taskList);
        updateTaskOverview();
    }

    private void loadTasksForDate(String date) {
        List<TaskEntity> allTasks = taskDao.getAllTasks();
        taskList = new ArrayList<>();

        for (TaskEntity task : allTasks) {
            if (date.equals(task.dueDate)) {
                taskList.add(task);
            }
        }
    }

    private void updateTaskOverview() {
        int totalTasks = taskDao.getAllTasks().size();
        int completedTasks = 0;
        for (TaskEntity task : taskDao.getAllTasks()) {
            if (task.isCompleted) {
                completedTasks++;
            }
        }
        textTotalTasks.setText(String.valueOf(totalTasks));
        textCompletedTasks.setText(String.valueOf(completedTasks));
    }

    private void updateTodayDate(String date) {
        textTodayTask.setText("Tasks - " + date);
    }

    private String getTodayDate() {
        return new SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(new Date());
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
                    selectedDate = sdf.format(calendar.getTime());

                    // Update title
                    updateTodayDate(selectedDate);

                    // Load new tasks
                    loadTasksForDate(selectedDate);
                    adapter.setTaskList(taskList);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText editTaskName = dialogView.findViewById(R.id.editTaskName);
        TextView textDueDate = dialogView.findViewById(R.id.textDueDate);
        Button btnPickDate = dialogView.findViewById(R.id.btnPickDate);
        Button btnSave = dialogView.findViewById(R.id.btnSaveTask);

        final Calendar selectedCalendar = Calendar.getInstance();

        btnPickDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        selectedCalendar.set(Calendar.YEAR, year);
                        selectedCalendar.set(Calendar.MONTH, month);
                        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
                        textDueDate.setText(sdf.format(selectedCalendar.getTime()));
                    },
                    selectedCalendar.get(Calendar.YEAR),
                    selectedCalendar.get(Calendar.MONTH),
                    selectedCalendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String taskName = editTaskName.getText().toString().trim();
            String dueDate = textDueDate.getText().toString();

            if (!taskName.isEmpty() && !dueDate.isEmpty()) {
                TaskEntity newTask = new TaskEntity(taskName, "", -1, dueDate, false);
                newTask.taskType = "manual";
                taskDao.insertTask(newTask);

                reloadTasksAfterCheckboxChange();
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}