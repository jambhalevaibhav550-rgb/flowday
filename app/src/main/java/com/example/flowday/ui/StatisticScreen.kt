package com.example.flowday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.util.*

enum class StatsView { WEEKLY, MONTHLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticScreen(viewModel: TaskViewModel = viewModel(), onMenuClick: () -> Unit) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    
    // State Management for view toggle
    var currentView by remember { mutableStateOf(StatsView.WEEKLY) }
    
    // Logic Calculations
    val now = LocalDate.now()
    
    // Time Analysis & Data Calculation
    val filteredTasks = remember(tasks, currentView) {
        when (currentView) {
            StatsView.WEEKLY -> {
                val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                tasks.filter { task ->
                    val taskDate = Instant.ofEpochMilli(task.date).atZone(ZoneId.systemDefault()).toLocalDate()
                    !taskDate.isBefore(startOfWeek) && !taskDate.isAfter(endOfWeek)
                }
            }
            StatsView.MONTHLY -> {
                tasks.filter { task ->
                    val taskDate = Instant.ofEpochMilli(task.date).atZone(ZoneId.systemDefault()).toLocalDate()
                    taskDate.month == now.month && taskDate.year == now.year
                }
            }
        }
    }

    // Summing up 'Execution Time' (Assuming it represents duration in minutes for logic)
    val totalExecutionTime = filteredTasks.sumOf { it.executionTime }
    
    // Completion Status Logic
    val totalTasks = filteredTasks.size
    val completedTasks = filteredTasks.count { it.status == 1 }
    val completionPercentage = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks * 100).toInt() else 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Data Statistics", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // View Toggle State Management
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { currentView = StatsView.WEEKLY },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentView == StatsView.WEEKLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (currentView == StatsView.WEEKLY) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Weekly")
                    }
                    Button(
                        onClick = { currentView = StatsView.MONTHLY },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentView == StatsView.MONTHLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (currentView == StatsView.MONTHLY) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Monthly")
                    }
                }
            }

            // Stats Summary Grid
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        title = "Total Time",
                        value = "${totalExecutionTime}m",
                        trend = "+5%",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Tasks",
                        value = "$totalTasks",
                        trend = "-2%",
                        isNegative = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Charts Section (Visual representation)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = if (currentView == StatsView.WEEKLY) "Weekly Trends" else "Monthly Trends",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "$totalExecutionTime min",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+12.5%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        
                        // Fake Graph placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .padding(vertical = 16.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), Color.Transparent)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                        
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            val labels = if (currentView == StatsView.WEEKLY) listOf("M", "T", "W", "T", "F", "S", "S") else listOf("W1", "W2", "W3", "W4")
                            labels.forEach { label ->
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Goal Tracking Section (Completion Status Logic)
            item {
                Text("Goal Tracking", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProgressItem(
                        icon = Icons.Default.Flag,
                        label = "Completion Rate",
                        progress = completionPercentage / 100f,
                        color = Color(0xFF6366F1)
                    )
                    ProgressItem(
                        icon = Icons.Default.Bolt,
                        label = "Efficiency Rate",
                        progress = 0.62f, // Placeholder for other logic
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, trend: String, isNegative: Boolean = false, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(trend, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isNegative) Color.Red else Color(0xFF10B981))
            }
        }
    }
}

@Composable
fun ProgressItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, progress: Float, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    }
                    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color,
                trackColor = color.copy(alpha = 0.1f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
