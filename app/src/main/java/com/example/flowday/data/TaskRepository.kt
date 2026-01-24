package com.example.flowday.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    // We need to react to auth changes. 
    // Since Firebase.auth.currentUser is not a Flow, we might need a way to trigger updates.
    // However, usually ViewModel handles the "identity". 
    // Better approach for Repository: Expose a function `getTasksForUser` OR 
    // make `allTasks` a flow that flatMaps the auth state if available.
    // Given the architecture, let's expose a method to get flow, and let ViewModel switch it.
    
    fun getTasksForUser(userId: String): Flow<List<Task>> = taskDao.getAllTasks(userId)
    
    // Kept for backward compatibility if needed, but returning empty or throwing might be better
    // helping strict isolation.
    // val allTasks: Flow<List<Task>> = taskDao.getAllTasks() // REMOVED to force usage of user-specific flow

    suspend fun insertTask(task: Task) = withContext(Dispatchers.IO) {
        // Assign userId if logged in
        val user = auth.currentUser
        val taskWithUser = if (user != null) task.copy(userId = user.uid) else task
        
        taskDao.insertTask(taskWithUser)
        
        if (user != null) {
            syncTaskToFirestore(taskWithUser)
        }
    }

    suspend fun deleteTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
        auth.currentUser?.let { user ->
            // Use task.id as the document ID
            try {
                firestore.collection("tasks").document(task.id)
                    .delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun markPastTasksFailed(timestamp: Long) {
        auth.currentUser?.let { user ->
            taskDao.markPastTasksFailed(timestamp, user.uid)
            
            // Sync with Firestore
            val snapshot = firestore.collection("users").document(user.uid)
                 .collection("tasks")
                 .whereLessThan("date", timestamp)
                 .whereEqualTo("status", 0)
                 .get().await()
             
             for (doc in snapshot.documents) {
                 doc.reference.update("status", 2)
             }
        }
    }

    


    // Manual Sync function called on Login
    suspend fun syncTasksFromFirestore() {
        val user = auth.currentUser
        if (user != null) {
            try {
                val snapshot = firestore.collection("users").document(user.uid)
                    .collection("tasks").get().await()
                
                val remoteTasks = snapshot.toObjects(Task::class.java)
                for (task in remoteTasks) {
                    taskDao.insertTask(task)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun syncTaskToFirestore(task: Task) {
        task.userId?.let { uid ->
            // Use task.id as document ID
            firestore.collection("tasks").document(task.id)
                .set(task)
        }
    }

    fun startRealtimeSync(userId: String) {
        listenerRegistration?.remove()
        
        listenerRegistration = firestore.collection("tasks")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    // OPTIMIZATION: Ignore cache updates to prevent "echo" loops and UI flickering
                    // Since we map Room as the single source of truth and update it optimistically,
                    // we only care about SERVER updates here.
                    if (snapshots.metadata.isFromCache) {
                        return@addSnapshotListener
                    }

                    // Use a safe scope for listening, though listener runs on main/worker usually.
                    // We launch IO for the heavy processing (DB Ops and Deserialization)
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        for (dc in snapshots.documentChanges) {
                            try {
                                // SAFE DESERIALIZATION
                                val remoteTask = dc.document.toObject(Task::class.java).copy(id = dc.document.id)
                                
                                when (dc.type) {
                                    com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                        // Room's OnConflictStrategy.REPLACE will handle UPSERT using the ID
                                        taskDao.insertTask(remoteTask)
                                    }
                                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                        taskDao.deleteTask(remoteTask)
                                    }
                                }
                            } catch (e: Exception) {
                                // Log validation error but DO NOT CRASH APP
                                e.printStackTrace()
                                println("Skipping corrupted task: ${dc.document.id}")
                            }
                        }
                    }
                }
            }
    }

    fun stopRealtimeSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    suspend fun migrateAnonymousTasks(userId: String) = withContext(Dispatchers.IO) {
        taskDao.migrateTasks(userId)
    }
}
