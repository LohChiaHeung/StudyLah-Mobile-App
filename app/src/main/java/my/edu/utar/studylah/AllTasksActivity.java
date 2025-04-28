package my.edu.utar.studylah;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllTasksActivity extends AppCompatActivity {

    private RecyclerView recyclerUpcoming, recyclerCompleted;
    private TaskAdapterUpcoming upcomingAdapter;
    private TaskAdapterCompleted completedAdapter;
    private TaskDao taskDao;
    private AppDatabase db;
    private List<TaskEntity> upcomingTasks;
    private List<TaskEntity> completedTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_tasks);

        db = AppDatabase.getInstance(this);
        taskDao = db.taskDao();

        ImageButton btnBack = findViewById(R.id.btnBackAllTasks);
        btnBack.setOnClickListener(v -> finish());

        recyclerUpcoming = findViewById(R.id.recyclerUpcoming);
        recyclerCompleted = findViewById(R.id.recyclerCompleted);

        setupRecyclerViews();
    }

    private void setupRecyclerViews() {
        upcomingTasks = new ArrayList<>();
        completedTasks = new ArrayList<>();

        List<TaskEntity> allTasks = taskDao.getAllTasks();

        for (TaskEntity task : allTasks) {
            if (task.isCompleted) {
                completedTasks.add(task);
            } else {
                upcomingTasks.add(task);
            }
        }

        // ✨ Sort upcoming tasks by due date (ascending)
        Collections.sort(upcomingTasks, (task1, task2) -> compareDates(task1.dueDate, task2.dueDate));

        // ✨ Sort completed tasks by completed date (descending)
        Collections.sort(completedTasks, (task1, task2) -> compareDates(task2.dueDate, task1.dueDate));

        // Setup RecyclerViews
        recyclerUpcoming.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columns
        upcomingAdapter = new TaskAdapterUpcoming(this, upcomingTasks);
        recyclerUpcoming.setAdapter(upcomingAdapter);

        recyclerCompleted.setLayoutManager(new LinearLayoutManager(this)); // Vertical list
        completedAdapter = new TaskAdapterCompleted(this, completedTasks);
        recyclerCompleted.setAdapter(completedAdapter);
    }

    private int compareDates(String dateStr1, String dateStr2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
            Date date1 = sdf.parse(dateStr1);
            Date date2 = sdf.parse(dateStr2);
            return date1.compareTo(date2);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
