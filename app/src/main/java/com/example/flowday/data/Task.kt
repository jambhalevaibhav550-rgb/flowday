package com.example.flowday.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val date: Long, // Creation timestamp
    val validity: String, // Description text (e.g., "Valid until...")
    val executionTime: Long, // Time of day (HH:mm) stored as timestamp/epoch
    val isCompleted: Boolean = false,
    
    // New fields for recurrence logic
    val recurrenceType: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY
    val recurrenceDays: String = "", // Comma-separated days for weekly (e.g., "1,3,5" for Mon, Wed, Fri)
    val validUntil: Long = Long.MAX_VALUE // End date timestamp for recurrence
)
