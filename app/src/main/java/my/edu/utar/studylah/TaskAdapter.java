package my.edu.utar.studylah;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<TaskEntity> taskList;
    private Context context;
    private TaskDao taskDao;

    public TaskAdapter(Context context, List<TaskEntity> taskList, TaskDao taskDao) {
        this.context = context;
        this.taskList = taskList;
        this.taskDao = taskDao;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName, taskDate;
        CheckBox checkBox;

        public TaskViewHolder(View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.taskName);
            taskDate = itemView.findViewById(R.id.taskDate);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TaskViewHolder holder, int position) {
        TaskEntity task = taskList.get(position);

        holder.taskName.setText(task.taskName);
        holder.taskDate.setText("Due: " + task.dueDate);
        holder.checkBox.setChecked(task.isCompleted);

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.isCompleted = isChecked;
            taskDao.markTaskComplete(task.taskId);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public TaskEntity getTaskAt(int position) {
        return taskList.get(position);
    }

    public void removeTask(int position) {
        taskList.remove(position);
        notifyItemRemoved(position);
    }

    public void addTask(int position, TaskEntity task) {
        taskList.add(position, task);
        notifyItemInserted(position);
    }
}