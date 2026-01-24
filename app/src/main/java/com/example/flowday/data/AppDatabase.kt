package com.example.flowday.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
