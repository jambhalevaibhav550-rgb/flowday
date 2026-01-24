package com.example.flowday.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY date ASC")
    fun getAllTasks(userId: String): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET status = 2 WHERE status = 0 AND date < :timestamp AND userId = :userId")
    suspend fun markPastTasksFailed(timestamp: Long, userId: String)

    @Query("UPDATE tasks SET userId = :newUserId WHERE userId IS NULL OR userId = ''")
    suspend fun migrateTasks(newUserId: String)
}
