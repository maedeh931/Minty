package com.minty.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.minty.app.databinding.HistoryItemBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Custom ListView adapter for displaying completed tasks in Minty.
 * Implements the ViewHolder pattern using View Binding (HistoryItemBinding).
 */
public class HistoryAdapter extends BaseAdapter {

    private List<Task> tasks;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());

    public HistoryAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return tasks != null ? tasks.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return tasks != null ? tasks.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return tasks != null ? tasks.get(position).getId() : position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            HistoryItemBinding binding = HistoryItemBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            convertView = binding.getRoot();
            holder = new ViewHolder(binding);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Task task = tasks.get(position);
        holder.binding.tvHistoryTaskTitle.setText(task.getTitle());

        String minutesText = "+" + task.getMinutes() + " min";
        holder.binding.tvHistoryMinutes.setText(minutesText);

        // Format and display the completion timestamp
        if (task.getCompletedAt() > 0) {
            holder.binding.tvCompletedDate.setText(dateFormat.format(new Date(task.getCompletedAt())));
        } else {
            holder.binding.tvCompletedDate.setText("");
        }

        return convertView;
    }

    static class ViewHolder {
        final HistoryItemBinding binding;

        ViewHolder(HistoryItemBinding binding) {
            this.binding = binding;
        }
    }
}
