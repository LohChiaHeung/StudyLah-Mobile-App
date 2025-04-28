package my.edu.utar.studylah;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapterUpcoming extends RecyclerView.Adapter<TaskAdapterUpcoming.ViewHolder> {

    private Context context;
    private List<TaskEntity> taskList;
    private TaskDao taskDao;

    public TaskAdapterUpcoming(Context context, List<TaskEntity> taskList) {
        this.context = context;
        this.taskList = taskList;
        this.taskDao = AppDatabase.getInstance(context).taskDao();

        sortTasksByDueDate();
    }

    @Override
    public TaskAdapterUpcoming.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task_upcoming, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TaskAdapterUpcoming.ViewHolder holder, int position) {
        TaskEntity task = taskList.get(position);

        holder.textTaskName.setText(task.taskName);
        holder.textDueDate.setText("Due: " + task.dueDate);

        holder.itemView.setOnClickListener(v -> {
            showDeleteConfirmationDialog(holder.getAdapterPosition(), holder.itemView);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    private void sortTasksByDueDate() {
        Collections.sort(taskList, (task1, task2) -> compareDates(task1.dueDate, task2.dueDate));
    }

    private int compareDates(String dateStr1, String dateStr2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
            Date date1 = sdf.parse(dateStr1);
            Date date2 = sdf.parse(dateStr2);
            return date1.compareTo(date2);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void showDeleteConfirmationDialog(int position, View view) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    TaskEntity deletedTask = taskList.get(position);

                    taskDao.deleteTask(deletedTask.taskId);
                    taskList.remove(position);
                    notifyItemRemoved(position);

                    Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show();

                    Snackbar.make(view, "Task deleted", Snackbar.LENGTH_LONG)
                            .setAction("UNDO", v -> {
                                taskDao.insertTask(deletedTask);
                                taskList.add(deletedTask);
                                sortTasksByDueDate();
                                notifyDataSetChanged();
                            }).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTaskName, textDueDate;

        public ViewHolder(View itemView) {
            super(itemView);
            textTaskName = itemView.findViewById(R.id.textTaskName);
            textDueDate = itemView.findViewById(R.id.textDueDate);
        }
    }
}
