package com.example.flowday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowday.data.Task
import com.example.flowday.data.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTask(
        name: String, 
        recurrenceType: String, 
        recurrenceDays: String, 
        validUntil: Long, 
        executionTime: Long
    ) {
        viewModelScope.launch {
            val task = Task(
                name = name,
                date = System.currentTimeMillis(), // Creation date
                validity = if (recurrenceType == "NONE") "One Time" else recurrenceType, // Simple validity text
                executionTime = executionTime,
                recurrenceType = recurrenceType,
                recurrenceDays = recurrenceDays,
                validUntil = validUntil
            )
            repository.insertTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.insertTask(task.copy(isCompleted = !task.isCompleted))
        }
    }
}
