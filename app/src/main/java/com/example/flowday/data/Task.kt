package com.example.flowday.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val date: Long = 0L, // Creation timestamp
    val validity: String = "", // Description text (e.g., "Valid until...")
    val executionTime: Long = 0L, // Time of day (HH:mm) stored as timestamp/epoch
    val status: Int = 0, // 0 = Active, 1 = Completed, 2 = Failed, 3 = CarryForward
    val userId: String? = null, // For Firebase syncing
    
    // New fields for recurrence logic
    val recurrenceType: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY
    val recurrenceDays: String = "", // Comma-separated days for weekly (e.g., "1,3,5" for Mon, Wed, Fri)
    val validUntil: Long = Long.MAX_VALUE // End date timestamp for recurrence
)
