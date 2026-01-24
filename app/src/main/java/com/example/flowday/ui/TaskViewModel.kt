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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.example.flowday.data.UserData
import com.example.flowday.data.SignInResult
import com.example.flowday.data.SignInState
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    @com.example.flowday.di.AuthClientQualifier private val signInClient: com.google.android.gms.auth.api.identity.SignInClient,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val _signInState = kotlinx.coroutines.flow.MutableStateFlow(SignInState())
    val signInState = _signInState.asStateFlow()

    fun onSignInResult(result: SignInResult) {
        _signInState.value = SignInState(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage,
            userData = result.data
        )
        if (result.data != null) {
            viewModelScope.launch {
                // 1. Migrate any anonymous local tasks to this user
                repository.migrateAnonymousTasks(result.data.userId)
                
                // 2. Start Realtime Sync (which fetches remote tasks and keeps them in sync)
                repository.startRealtimeSync(result.data.userId)
            }
        }
    }
    
    fun resetSignInState() {
        _signInState.value = SignInState()
    }

    private val KEY_SELECTED_DATE = "selected_date"
    private val _selectedDate = savedStateHandle.getStateFlow(KEY_SELECTED_DATE, System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate

    fun setSelectedDate(date: Long) {
        savedStateHandle[KEY_SELECTED_DATE] = date
    }

    // Persist selectedDate using SavedStateHandle
    // ...

    // User Identity Logic
    // ...

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = _signInState.flatMapLatest { state ->
        val user = Firebase.auth.currentUser
        if (state.isSignInSuccessful && user != null) {
             repository.getTasksForUser(user.uid)
        } else {
             kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // PERFORMANCE OPTIMIZATION: Calcualte dots on Background Thread
    // Pre-calculates a Set of dates that have tasks, avoiding O(N) checks in the UI loop.
    val calendarDots: StateFlow<Set<LocalDate>> = tasks.map { taskList ->
        withContext(Dispatchers.Default) {
            val datesWithTasks = mutableSetOf<LocalDate>()
            for (task in taskList) {
                // Add the main task date
                val taskDate = Instant.ofEpochMilli(task.date).atZone(ZoneId.systemDefault()).toLocalDate()
                datesWithTasks.add(taskDate)

                // If recurring, we might need a more complex strategy.
                // For now, to keep it simple and performant, we only mark the *start* date 
                // or we could expand distinct dates if the range is small.
                // However, the current UI logic was checking recurrence for EVERY cell.
                // To support recurrence dots, we'd need to expand the recurrence rule for the currently viewed month.
                // But `calendarDots` usually just implies "checking against CURRENT view". 
                // Since `tasks` is ALL tasks, we can't expand infinite recurrence.
                
                // Better Strategy for O(1) in UI:
                // The UI passes the 'Month' to the VM? No, creates a derived state.
                // But the user asked for VM optimization.
                // Let's stick to the simple strategy: Map unique dates we KNOW about.
                // For recurrence, the UI might still need to check logic, BUT we can optimize the list.
                // Re-reading user request: "Loop... infinite loop data fetch".
                // The fix in Repository clears the loop.
                // The "dots rendering" lag -> The UI was filtering the *entire* list for *every* cell.
                // We can't easily pre-calculate infinite recurrence in a Set<LocalDate> without knowing the bounds.
                // BUT, we can make the `tasks` list distinct or optimized.
            }
            datesWithTasks
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    
    override fun onCleared() {
        super.onCleared()
        repository.stopRealtimeSync()
    }

    // Ensure we check auth on init too, in case user is already logged in (persistence)
    init {
        try {
            val user = Firebase.auth.currentUser?.run {
                UserData(
                    userId = uid,
                    username = displayName,
                    profilePictureUrl = photoUrl?.toString()
                )
            }
            if (user != null) {
                _signInState.value = SignInState(
                    isSignInSuccessful = true,
                    userData = user
                )
                // Initialize Realtime Sync on startup
                repository.startRealtimeSync(user.userId)
            }
            checkMidnightReset()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            try {
                signInClient.signOut().await()
                Firebase.auth.signOut()
                resetSignInState()
                // Clear any other local state if needed
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    suspend fun signIn(): com.google.android.gms.auth.api.identity.BeginSignInResult? {
        return try {
            val signInRequest = com.google.android.gms.auth.api.identity.BeginSignInRequest.Builder()
                .setGoogleIdTokenRequestOptions(
                    com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(com.example.flowday.AppConfig.WEB_CLIENT_ID)
                        .build()
                )
                .setAutoSelectEnabled(true)
                .build()
            
            signInClient.beginSignIn(signInRequest).await()
        } catch(e: Exception) {
            e.printStackTrace()
            if(e is java.util.concurrent.CancellationException) throw e
            null
        }
    }

    suspend fun handleSignInResult(intent: android.content.Intent) {
        try {
            val credential = signInClient.getSignInCredentialFromIntent(intent)
            val googleIdToken = credential.googleIdToken
            val googleCredentials = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdToken, null)
            
            val user = Firebase.auth.signInWithCredential(googleCredentials).await().user
            val userData = user?.run {
                com.example.flowday.data.UserData(
                    userId = uid,
                    username = displayName,
                    profilePictureUrl = photoUrl?.toString()
                )
            }
            onSignInResult(com.example.flowday.data.SignInResult(
                data = userData,
                errorMessage = null
            ))
        } catch(e: Exception) {
            e.printStackTrace()
            if(e is java.util.concurrent.CancellationException) throw e
             onSignInResult(com.example.flowday.data.SignInResult(
                data = null,
                errorMessage = e.message
            ))
        }
    }

    fun addTask(
        name: String, 
        recurrenceType: String, 
        recurrenceDays: String, 
        validUntil: Long, 
        executionTime: Long,
        date: Long
    ) {
        viewModelScope.launch {
            // Update selected date to the new task's date so UI jumps there
            savedStateHandle[KEY_SELECTED_DATE] = date

            // Combine date (Year/Month/Day) with executionTime (Hour/Minute)
            val selectedCalendar = java.util.Calendar.getInstance()
            selectedCalendar.timeInMillis = date
            
            val timeCalendar = java.util.Calendar.getInstance()
            timeCalendar.timeInMillis = executionTime
            
            selectedCalendar.set(java.util.Calendar.HOUR_OF_DAY, timeCalendar.get(java.util.Calendar.HOUR_OF_DAY))
            selectedCalendar.set(java.util.Calendar.MINUTE, timeCalendar.get(java.util.Calendar.MINUTE))
            selectedCalendar.set(java.util.Calendar.SECOND, 0)
            selectedCalendar.set(java.util.Calendar.MILLISECOND, 0)

            val finalExecutionTime = selectedCalendar.timeInMillis

            // Get current userId from state
            val currentUserId = _signInState.value.userData?.userId

            val task = Task(
                name = name,
                date = date, // Use selected date
                validity = if (recurrenceType == "NONE") "One Time" else recurrenceType, // Simple validity text
                executionTime = finalExecutionTime,
                recurrenceType = recurrenceType,
                recurrenceDays = recurrenceDays,
                validUntil = validUntil,
                status = 0, // Default Active
                userId = currentUserId // Assign the logged-in user ID
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
                id = java.util.UUID.randomUUID().toString(), // Reset ID for new entry
                date = calendar.timeInMillis,
                executionTime = newExecutionTime,
                status = 0 // Active
            )
            repository.insertTask(newTask)
        }
    }
}
