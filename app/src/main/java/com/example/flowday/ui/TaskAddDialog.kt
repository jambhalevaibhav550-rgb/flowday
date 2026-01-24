package com.example.flowday.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAddDialog(
    initialDate: Long, // Kept for API compatibility but IGNORING it for default as per instruction
    onDismiss: () -> Unit,
    onSave: (name: String, recurrenceType: String, recurrenceDays: String, validUntil: Long, executionTime: Long, taskDate: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    // Requirement: Default to Today, independent of background selection
    var taskDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var recurrenceType by remember { mutableStateOf("NONE") } // NONE, DAILY, WEEKLY, MONTHLY
    val selectedDays = remember { mutableStateListOf<Int>() } // 1=Mon, 2=Tue... 7=Sun
    var validUntil by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.MONTH, 1) }.timeInMillis) }
    var executionTime by remember { 
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }.timeInMillis)
    }

    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    // Validation Logic
    val isPastDate = remember(taskDate, executionTime) {
        val taskCal = Calendar.getInstance().apply { timeInMillis = taskDate }
        val timeCal = Calendar.getInstance().apply { timeInMillis = executionTime }
        
        taskCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
        taskCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
        
        taskCal.timeInMillis < System.currentTimeMillis() - 60000 // 1 minute buffer
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Smart Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Task Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    placeholder = { Text("e.g. Morning Yoga") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Row: Date & Time (Compact UI)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // DATE PICKER
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Date", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        OutlinedCard(
                            onClick = {
                                val calendar = Calendar.getInstance().apply { timeInMillis = taskDate }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val newDate = Calendar.getInstance().apply {
                                            set(y, m, d)
                                        }
                                        taskDate = newDate.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dateFormatter.format(Date(taskDate)), fontSize = 12.sp) // Reduced font size for compact
                            }
                        }
                    }

                    // TIME PICKER
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Time", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        OutlinedCard(
                            onClick = {
                                val calendar = Calendar.getInstance().apply { timeInMillis = executionTime }
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        val newTime = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, h)
                                            set(Calendar.MINUTE, m)
                                        }
                                        executionTime = newTime.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(timeFormatter.format(Date(executionTime)), fontSize = 12.sp)
                            }
                        }
                    }
                }

                // WARNING Message
                if (isPastDate) {
                    val taskCal = Calendar.getInstance().apply { timeInMillis = taskDate }
                    val nowCal = Calendar.getInstance()
                    val isToday = taskCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                                  taskCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
                    
                    Text(
                        text = if (isToday) "Bhai, select a future time for today!" else "Please select a current or future date.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Recurrence Type
                Text("Repetition", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("NONE", "DAILY", "WEEKLY", "MONTHLY").forEach { type ->
                        FilterChip(
                            selected = recurrenceType == type,
                            onClick = { recurrenceType = type },
                            label = { Text(type.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Weekly Day Selection
                if (recurrenceType == "WEEKLY") {
                    Text("Repeat on", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = listOf("M", "T", "W", "T", "F", "S", "S")
                        days.forEachIndexed { index, day ->
                            val dayNum = index + 1
                            val isSelected = selectedDays.contains(dayNum)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (isSelected) selectedDays.remove(dayNum) else selectedDays.add(dayNum)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                // UNTIL Date Picker (Only if Recurrence is enabled)
                if (recurrenceType != "NONE") {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Until", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            OutlinedCard(
                                onClick = {
                                    val calendar = Calendar.getInstance().apply { timeInMillis = validUntil }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val newDate = Calendar.getInstance().apply {
                                                set(y, m, d)
                                            }
                                            validUntil = newDate.timeInMillis
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(dateFormatter.format(Date(validUntil)), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isPastDate) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val daysString = selectedDays.sorted().joinToString(",")
                            onSave(name, recurrenceType, daysString, validUntil, executionTime, taskDate)
                        }
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

