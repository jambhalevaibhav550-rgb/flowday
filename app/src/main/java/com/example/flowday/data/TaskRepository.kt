package com.example.flowday.data

import kotlinx.coroutines.flow.Flow
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
    private val firestore = com.google.firebase.ktx.Firebase.firestore
    private val auth = com.google.firebase.auth.ktx.auth

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insertTask(task: Task) {
        // Assign userId if logged in
        val user = auth.currentUser
        val taskWithUser = if (user != null) task.copy(userId = user.uid) else task
        
        taskDao.insertTask(taskWithUser)
        
        if (user != null) {
            syncTaskToFirestore(taskWithUser)
        }
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        auth.currentUser?.let { user ->
            firestore.collection("users").document(user.uid)
                .collection("tasks").document(task.date.toString()) // Using date as ID for simplicity or generated ID if available
                .delete()
        }
    }

    suspend fun markPastTasksFailed(timestamp: Long) {
        taskDao.markPastTasksFailed(timestamp)
        // Note: Bulk update in Firestore would require a batch write or Query.
        // For simplicity, we might just rely on local logic or update when app opens.
        // Or strictly:
        auth.currentUser?.let { user ->
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
        auth.currentUser?.let { user ->
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
        // We use 'date' as document ID for uniqueness in this simplistic model, 
        // ideally 'id' (autoGen) matches or we use a UUID string. 
        // Since 'id' is local auto-gen, we should perhaps rely on a stable ID or just use timestamp if unique enough.
        // Let's us creation 'date' which is a timestamp.
        task.userId?.let { uid ->
            firestore.collection("users").document(uid)
                .collection("tasks").document(task.date.toString())
                .set(task)
        }
    }
}
