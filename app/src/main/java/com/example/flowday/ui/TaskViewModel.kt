package com.example.flowday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowday.data.Task
import com.example.flowday.data.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.flowday.sign_in.GoogleAuthClient
import com.example.flowday.sign_in.SignInResult
import com.example.flowday.sign_in.SignInState

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    @com.example.flowday.di.AuthClientQualifier private val googleAuthClient: com.example.flowday.sign_in.GoogleAuthClient // We will need to module this.
    // For now, assuming we can get it or just pass it to functions, but Hilt is better.
    // Let's create a temporary module or just instantiate if Hilt setup is complex for this step.
    // simpler: "val googleAuthClient = ..." inside if context available? No, VM shouldn't hold context.
    // User asked for "Implement the real GoogleSignInClient".
    // I will add a set functions or expect it injected.
    // Let's stick to standard flow: The Activity handles the Result, writes to VM.
) : ViewModel() {

    private val _signInState = kotlinx.coroutines.flow.MutableStateFlow(com.example.flowday.sign_in.SignInState())
    val signInState = _signInState.asStateFlow()

    fun onSignInResult(result: com.example.flowday.sign_in.SignInResult) {
        _signInState.value = com.example.flowday.sign_in.SignInState(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage
        )
        if (result.data != null) {
            viewModelScope.launch {
                repository.syncTasksFromFirestore()
            }
        }
    }
    
    fun resetSignInState() {
        _signInState.value = com.example.flowday.sign_in.SignInState()
    }
    
    // Existing code...

    private val _selectedDate = kotlinx.coroutines.flow.MutableStateFlow(System.currentTimeMillis())
    val selectedDate = _selectedDate.asStateFlow()

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
    }

    // Existing code...

    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkMidnightReset()
    }

    private fun checkMidnightReset() {
        viewModelScope.launch {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            repository.markPastTasksFailed(calendar.timeInMillis)
        }
    }

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
                date = _selectedDate.value, // Use selected date
                validity = if (recurrenceType == "NONE") "One Time" else recurrenceType, // Simple validity text
                executionTime = executionTime,
                recurrenceType = recurrenceType,
                recurrenceDays = recurrenceDays,
                validUntil = validUntil,
                status = 0 // Default Active
            )
            repository.insertTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun updateTaskStatus(task: Task, newStatus: Int) {
        viewModelScope.launch {
            repository.insertTask(task.copy(status = newStatus))
        }
    }

    fun carryForwardTask(task: Task) {
        viewModelScope.launch {
            // 1. Mark current as CarryForward (3)
            repository.insertTask(task.copy(status = 3))

            // 2. Create new task for tomorrow
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = task.date
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            
            // Adjust execution time as well if it's relevant, or just keep the same HH:mm relative to date?
            // executionTime seems to be a timestamp in the code. If it's a timestamp, we might need to add 24h.
            // But let's check how executionTime is used. usually "Time of day".
            // If it's a full timestamp, we should shift it. If it's just HH:mm offset, it might depend.
            // User comment: "Time of day (HH:mm) stored as timestamp/epoch"
            // If it's epoch, we should shift it by 1 day.
            
            val newExecutionTime = task.executionTime + (24 * 60 * 60 * 1000)

            val newTask = task.copy(
                id = 0, // Reset ID for new entry
                date = calendar.timeInMillis,
                executionTime = newExecutionTime,
                status = 0 // Active
            )
            repository.insertTask(newTask)
        }
    }
}
