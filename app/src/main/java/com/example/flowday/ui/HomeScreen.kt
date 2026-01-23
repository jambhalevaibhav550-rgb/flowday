package com.example.flowday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flowday.data.Task
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: TaskViewModel = viewModel(), onMenuClick: () -> Unit) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    // Date Logic
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("EEE\ndd", Locale.getDefault()) }
    val todayString = remember { dateFormat.format(Date()) }
    
    // Convert selectedDate (Long) to String for grouping/filtering key
    val selectedDateString = remember(selectedDate) { dateFormat.format(Date(selectedDate)) }
    
    // Group tasks by date
    val groupedTasks = remember(tasks) {
        tasks.groupBy { dateFormat.format(Date(it.date)) }
    }
    
    // Get unique dates (sorted)
    val availableDates = remember(groupedTasks) {
        groupedTasks.keys.sorted().ifEmpty { listOf(todayString) }
    }

    // Filter tasks for selected date
    val displayedTasks = remember(groupedTasks, selectedDateString) {
        groupedTasks[selectedDateString] ?: emptyList()
    }

    // Compute Progress for TODAY
    val todayTasks = remember(groupedTasks, todayString) {
        groupedTasks[todayString] ?: emptyList()
    }
    val todayTotal = todayTasks.size
    val todayCompleted = todayTasks.count { it.status == 1 } // 1 = Completed
    val progress = if (todayTotal > 0) todayCompleted.toFloat() / todayTotal else 0f
    
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome, User", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF2B6CEE),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Timeline
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(availableDates) { dateString ->
                    val isSelected = dateString == selectedDateString
                    val dateObj = try { dateFormat.parse(dateString) } catch(e: Exception) { Date() }
                    
                    Column(
                        modifier = Modifier
                            .background(
                                if (isSelected) Color(0xFF2B6CEE) else Color.White,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { 
                                if (dateObj != null) {
                                    viewModel.setSelectedDate(dateObj.time)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = displayFormat.format(dateObj ?: Date()),
                            color = if (isSelected) Color.White else Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Progress Ring Section
                item { 
                     DailyGoalCard(progress = progress, completed = todayCompleted, total = todayTotal)
                }
                
                item {
                    Text(
                        text = if (selectedDateString == todayString) "Today's Tasks" else "Tasks for $selectedDateString",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF101622)
                    )
                }
                
                items(displayedTasks) { task ->
                    TaskItem(
                        task = task,
                        onDelete = { viewModel.deleteTask(task) },
                        onEdit = { /* Implement Edit Logic */ },
                        onDone = { viewModel.updateTaskStatus(task, 1) },
                        onFail = { viewModel.updateTaskStatus(task, 2) },
                        onCarryForward = { viewModel.carryForwardTask(task) }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        TaskAddDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, recurrenceType, recurrenceDays, validUntil, executionTime ->
                viewModel.addTask(name, recurrenceType, recurrenceDays, validUntil, executionTime)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DailyGoalCard(progress: Float, completed: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                CircularProgressIndicator(
                    progress = 1f,
                    color = Color.LightGray.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp
                )
                CircularProgressIndicator(
                    progress = progress,
                    color = Color(0xFF2B6CEE),
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Daily Progress", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("$completed / $total Tasks Completed", color = Color.Gray)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onFail: () -> Unit,
    onCarryForward: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showFailDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val cardColor = when (task.status) {
        1 -> Color(0xFFE8F5E9) // Green (Success)
        2 -> Color(0xFFFFEBEE) // Red (Failed)
        3 -> Color(0xFFFFFDE7) // Yellow (CarryForward)
        else -> Color.White // Active
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.name, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp,
                        style = if (task.status == 1) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle.Default
                    )
                    Text(
                        "${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(task.executionTime))} • ${task.validity}",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                
                // Status Icon or Buttons
                 if (task.status == 0) {
                     Row {
                         IconButton(onClick = onDone) {
                             Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = Color(0xFF4CAF50))
                         }
                         IconButton(onClick = { showFailDialog = true }) {
                             Icon(Icons.Default.Cancel, contentDescription = "Fail", tint = Color(0xFFF44336))
                         }
                     }
                 } else {
                     when(task.status) {
                         1 -> Icon(Icons.Default.Check, "Completed", tint = Color(0xFF4CAF50))
                         2 -> Icon(Icons.Default.Close, "Failed", tint = Color(0xFFF44336))
                         3 -> Icon(Icons.Default.Refresh, "Carried Forward", tint = Color(0xFFFFC107))
                     }
                 }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { 
                            onEdit()
                            showMenu = false 
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { 
                            showDeleteConfirm = true
                            showMenu = false 
                        }
                    )
                }
            }
        }
    }

    if (showFailDialog) {
        AlertDialog(
            onDismissRequest = { showFailDialog = false },
            title = { Text("Task Failed?") },
            text = { Text("Do you want to mark this task as Failed or Carry it Forward to tomorrow?") },
            confirmButton = {
                TextButton(onClick = { 
                    onFail()
                    showFailDialog = false
                }) {
                    Text("Mark Failed", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    onCarryForward()
                    showFailDialog = false
                }) {
                    Text("Carry Forward")
                }
            }
        )
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
