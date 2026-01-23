package com.example.flowday.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.flowday.R
import com.example.flowday.data.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val onDeleteClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMonth: TextView = itemView.findViewById(R.id.textMonth)
        private val textDay: TextView = itemView.findViewById(R.id.textDay)
        private val textName: TextView = itemView.findViewById(R.id.textName)
        private val textTime: TextView = itemView.findViewById(R.id.textTime)

        fun bind(task: Task) {
            val date = Date(task.date) // Using created date for display for now, or use executionTime
            textMonth.text = SimpleDateFormat("MMM", Locale.getDefault()).format(date).uppercase()
            textDay.text = SimpleDateFormat("dd", Locale.getDefault()).format(date)
            textName.text = task.name
            
            val execDate = Date(task.executionTime)
            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(execDate)
            textTime.text = "$time • ${task.validity}"
            
            // Simple delete on long click for now
            itemView.setOnLongClickListener {
                onDeleteClick(task)
                true
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
