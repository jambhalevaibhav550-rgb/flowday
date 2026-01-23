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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: TaskViewModel = viewModel(), onMenuClick: () -> Unit) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    
    // State variables for Task Manager logic
    var showAddDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMMM, dd", Locale.getDefault()) }

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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Text(
                        text = dateFormatter.format(Date()),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF101622)
                    )
                    Text(
                        text = "Good morning! You have ${tasks.size} tasks today.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            item { DailyGoalCard() }
            
            item {
                Column {
                    Text(
                        text = "Key Metrics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF101622)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard("Steps", "8,432", Color(0xFF10B981), modifier = Modifier.weight(1f))
                        MetricCard("Active", "45 min", Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                    }
                }
            }
            
            item {
                Text(
                    text = "Next Event",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101622)
                )
            }
            
            items(tasks) { task ->
                TaskItem(task)
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
fun DailyGoalCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2B6CEE), Color(0xFF1E4BB3))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("85%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Daily Goal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Activity Ring", color = Color.Gray)
                Text("You are almost there!", color = Color.Gray, fontSize = 12.sp)
                Button(
                    onClick = { },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B6CEE)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Details")
                }
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Simplified icon
                Box(modifier = Modifier.size(20.dp).background(color, RoundedCornerShape(4.dp)))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Medium, color = Color.Gray)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
fun TaskItem(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    SimpleDateFormat("MMM", Locale.getDefault()).format(Date(task.date)).uppercase(),
                    color = Color(0xFF2B6CEE),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    SimpleDateFormat("dd", Locale.getDefault()).format(Date(task.date)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(task.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(task.executionTime))} • ${task.validity}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}
