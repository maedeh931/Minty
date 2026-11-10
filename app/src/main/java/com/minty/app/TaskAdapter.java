package com.minty.app;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minty.app.databinding.TaskItemBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying planned and pending tasks in Minty.
 * Shows title, optional notes, date/time badge, category, priority, overdue status,
 * and popup actions (Move to Tomorrow, Reschedule, Focus Recovery, Delete).
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskCompleteListener {
        void onTaskComplete(Task task);
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface OnTaskMenuActionListener {
        void onEdit(Task task);
        void onMoveToTomorrow(Task task);
        void onReschedule(Task task);
        void onFocusRecovery(Task task);
        void onDelete(Task task);
    }

    private List<Task> tasks;
    private final OnTaskCompleteListener listener;
    private OnTaskClickListener itemClickListener;
    private OnTaskMenuActionListener menuActionListener;

    public TaskAdapter(List<Task> tasks, OnTaskCompleteListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    public void setOnTaskClickListener(OnTaskClickListener clickListener) {
        this.itemClickListener = clickListener;
    }

    public void setOnTaskMenuActionListener(OnTaskMenuActionListener menuListener) {
        this.menuActionListener = menuListener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TaskItemBinding binding = TaskItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        Context context = holder.itemView.getContext();

        holder.binding.tvTitle.setText(task.getTitle());
        String minutesText = "+" + task.getMinutes() + " min";
        holder.binding.tvMinutes.setText(minutesText);

        // Description / Notes
        if (!TextUtils.isEmpty(task.getDescription())) {
            holder.binding.tvDescription.setText(task.getDescription());
            holder.binding.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvDescription.setVisibility(View.GONE);
        }

        // Category Badge
        String category = task.getCategory();
        if (!TextUtils.isEmpty(category)) {
            holder.binding.tvCategoryBadge.setText(category);
            holder.binding.tvCategoryBadge.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvCategoryBadge.setVisibility(View.GONE);
        }

        // Time / Day Badge
        String timeBadgeText;
        if (task.getStartTimeMillis() > 0) {
            SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String timeStr = timeFmt.format(new Date(task.getStartTimeMillis()));
            if (task.isForToday()) {
                timeBadgeText = "Today • " + timeStr;
            } else if (task.isForTomorrow()) {
                timeBadgeText = "Tomorrow • " + timeStr;
            } else {
                SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d • ", Locale.getDefault());
                timeBadgeText = dateFmt.format(new Date(task.getTargetDateMillis())) + timeStr;
            }
        } else {
            if (task.isForToday()) {
                timeBadgeText = "Today ☀️";
            } else if (task.isForTomorrow()) {
                timeBadgeText = "Tomorrow 🌙";
            } else if (task.getTargetDateMillis() > 0) {
                SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d 📅", Locale.getDefault());
                timeBadgeText = dateFmt.format(new Date(task.getTargetDateMillis()));
            } else {
                timeBadgeText = "Today";
            }
        }
        holder.binding.tvTimeBadge.setText(timeBadgeText);

        // Overdue status
        if (task.isOverdue()) {
            holder.binding.tvOverdueBadge.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvOverdueBadge.setVisibility(View.GONE);
        }

        // Reset checked state without triggering old listeners
        holder.binding.cbComplete.setChecked(task.isCompleted());

        // When the checkbox is ticked, notify the listener to complete the task
        holder.binding.cbComplete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskComplete(task);
            }
        });

        // When the task card is tapped, notify the item click listener (e.g. to launch focus timer)
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onTaskClick(task);
            }
        });

        // Action menu button
        holder.binding.btnTaskMenu.setOnClickListener(v -> {
            showTaskPopupMenu(context, holder.binding.btnTaskMenu, task);
        });
    }

    private void showTaskPopupMenu(Context context, View anchor, Task task) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add(0, 5, 0, "Edit Task ✏️");
        popup.getMenu().add(0, 1, 1, "Move to Tomorrow 🌙");
        popup.getMenu().add(0, 2, 2, "Reschedule Date 📅");
        if (task.isOverdue()) {
            popup.getMenu().add(0, 3, 3, "Focus Recovery Assistant 💡");
        }
        popup.getMenu().add(0, 4, 4, "Delete Task 🗑️");

        popup.setOnMenuItemClickListener(item -> {
            if (menuActionListener == null) return false;
            switch (item.getItemId()) {
                case 5:
                    menuActionListener.onEdit(task);
                    return true;
                case 1:
                    menuActionListener.onMoveToTomorrow(task);
                    return true;
                case 2:
                    menuActionListener.onReschedule(task);
                    return true;
                case 3:
                    menuActionListener.onFocusRecovery(task);
                    return true;
                case 4:
                    menuActionListener.onDelete(task);
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        final TaskItemBinding binding;

        public TaskViewHolder(@NonNull TaskItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
