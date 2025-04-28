package my.edu.utar.studylah;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapterCompleted extends RecyclerView.Adapter<TaskAdapterCompleted.ViewHolder> {

    private Context context;
    private List<TaskEntity> completedTaskList;

    public TaskAdapterCompleted(Context context, List<TaskEntity> completedTaskList) {
        this.context = context;
        this.completedTaskList = completedTaskList;
    }

    @Override
    public TaskAdapterCompleted.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task_completed, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TaskAdapterCompleted.ViewHolder holder, int position) {
        TaskEntity task = completedTaskList.get(position);

        holder.textTaskName.setText(task.taskName);
        holder.textCompletedDate.setText("Completed: " + task.dueDate);

        holder.imageStatus.setImageResource(R.drawable.ic_correct);
    }

    @Override
    public int getItemCount() {
        return completedTaskList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageStatus;
        TextView textTaskName, textCompletedDate;

        public ViewHolder(View itemView) {
            super(itemView);
            imageStatus = itemView.findViewById(R.id.imageStatus);
            textTaskName = itemView.findViewById(R.id.textTaskName);
            textCompletedDate = itemView.findViewById(R.id.textCompletedDate);
        }
    }
}
