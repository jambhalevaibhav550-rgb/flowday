package com.example.flowday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flowday.data.Task
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun CalendarScreen(viewModel: TaskViewModel, onMenuClick: () -> Unit) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    // OPTIMIZATION: Collect pre-calculated dots
    val calendarDots by viewModel.calendarDots.collectAsState(initial = emptySet())
    
    // logic handling variables as requested
    val selectedDateMillis by viewModel.selectedDate.collectAsState()
    val selectedDate = remember(selectedDateMillis) {
        Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val today = remember { LocalDate.now() }

    // Jump to the new month when a date is selected (programmatically or manually)
    LaunchedEffect(selectedDate) {
        currentMonth = YearMonth.from(selectedDate)
    }
    
    // Date formatters
    val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    // Task filtering logic: taskDate == selectedDate
    // Helper function to check if a task occurs on a specific date
    fun isTaskScheduledOnDate(task: Task, date: LocalDate): Boolean {
        val taskStartDate = Instant.ofEpochMilli(task.date).atZone(ZoneId.systemDefault()).toLocalDate()
        val validUntilDate = Instant.ofEpochMilli(task.validUntil).atZone(ZoneId.systemDefault()).toLocalDate()

        if (date.isBefore(taskStartDate) || date.isAfter(validUntilDate)) {
            return false
        }

        return when (task.recurrenceType) {
            "DAILY" -> true
            "WEEKLY" -> {
                val dayOfWeek = date.dayOfWeek.value // 1=Mon, ... 7=Sun
                task.recurrenceDays.split(",").contains(dayOfWeek.toString())
            }
            "MONTHLY" -> taskStartDate.dayOfMonth == date.dayOfMonth
            else -> taskStartDate == date
        }
    }

    // Task filtering logic
    val filteredTasks = remember(tasks, selectedDate) {
        tasks.filter { task -> isTaskScheduledOnDate(task, selectedDate) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = currentMonth.format(monthYearFormatter),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                // Today Button
                IconButton(onClick = { viewModel.setSelectedDate(System.currentTimeMillis()) }) {
                    Icon(
                        Icons.Default.Today,
                        contentDescription = "Today",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            CalendarHeaderView()
            
            // Calendar Grid Logic
            CalendarGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                today = today,
                onDateSelected = { date ->
                    val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    viewModel.setSelectedDate(millis)
                },
                calendarDots = calendarDots
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            
            // Events List Section
            Text(
                text = "Events for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}",
                modifier = Modifier.padding(16.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (filteredTasks.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No events for this day", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(filteredTasks) { task ->
                        EventItemView(task, timeFormatter)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarHeaderView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        val days = listOf("S", "M", "T", "W", "T", "F", "S")
        days.forEach { day ->
            Text(
                text = day,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF92A4C9) // slate-400
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    calendarDots: Set<LocalDate> // OPTIMIZATION: Use Set for O(1) lookup
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    
    // Padding days from previous month to align with Sunday start
    val prevMonthPadding = firstDayOfMonth.dayOfWeek.value % 7
    val totalGridCells = 42 // 6 rows of 7 days
    
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dateInCell = firstDayOfMonth.minusDays(prevMonthPadding.toLong()).plusDays(cellIndex.toLong())
                    
                    val isCurrentMonth = dateInCell.month == currentMonth.month
                    val isSelected = dateInCell == selectedDate
                    val isToday = dateInCell == today
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable { onDateSelected(dateInCell) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // 1. Today Highlight (Persistent Soft Green)
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = Color(0xFFE8F5E9), // Light Green
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    )
                                }

                                // 2. Selection Highlight (Dark Blue overlay)
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    )
                                }

                                Text(
                                    text = dateInCell.dayOfMonth.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> Color(0xFF2E7D32) // Dark Green text for Today
                                        isCurrentMonth -> MaterialTheme.colorScheme.onBackground
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                            }
                            // Dot logic: O(1) Lookup
                            // Note: Recurrence logic for dots is simplified here to only show explicit task dates
                            // as pre-calculated in ViewModel for performance.
                            if (dateInCell in calendarDots) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(4.dp)
                                        .background(if(isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventItemView(task: Task, timeFormatter: DateTimeFormatter) {
    val executionTime = Instant.ofEpochMilli(task.executionTime)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            // Navigation logic detail connection: show execution time and validity
            Text(
                text = "${executionTime.format(timeFormatter)} • ${task.validity}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFCBD5E1))
    }
}
