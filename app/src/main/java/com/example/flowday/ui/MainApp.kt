package com.example.flowday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.launch

enum class AppScreen { HOME, CALENDAR, STATS }
enum class AppTheme { DEFAULT, DARK, GREEN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    // Global States
    var currentTheme by remember { mutableStateOf(AppTheme.DEFAULT) }
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Dynamic Color Schemes
    val colorScheme = when (currentTheme) {
        AppTheme.DEFAULT -> lightColorScheme(
            primary = Color(0xFF2B6CEE),
            onPrimary = Color.White,
            background = Color(0xFFF6F6F8),
            onBackground = Color(0xFF101622),
            surface = Color.White,
            onSurface = Color(0xFF101622)
        )
        AppTheme.DARK -> darkColorScheme(
            primary = Color(0xFF2B6CEE),
            onPrimary = Color.White,
            background = Color(0xFF101622),
            onBackground = Color.White,
            surface = Color(0xFF192233),
            onSurface = Color.White
        )
        AppTheme.GREEN -> lightColorScheme(
            primary = Color(0xFF2E7D32),
            onPrimary = Color.White,
            background = Color(0xFFF1F8E9),
            onBackground = Color(0xFF1B5E20),
            surface = Color.White,
            onSurface = Color(0xFF1B5E20)
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.background
                ) {
                    DrawerContent(
                        currentTheme = currentTheme,
                        onThemeSelected = { currentTheme = it },
                        onClose = { scope.launch { drawerState.close() } }
                    )
                }
            }
        ) {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        currentScreen = currentScreen,
                        onScreenSelected = { currentScreen = it }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (currentScreen) {
                        AppScreen.HOME -> HomeScreen(onMenuClick = { scope.launch { drawerState.open() } })
                        AppScreen.CALENDAR -> CalendarScreen(onMenuClick = { scope.launch { drawerState.open() } })
                        AppScreen.STATS -> StatisticScreen(onMenuClick = { scope.launch { drawerState.open() } })
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Profile Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Alex Johnson", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("john.doe@example.com", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Divider(color = Color.Gray.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(24.dp))

        Text("Environment Management", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        // Theme Selection Logic
        ThemeOption(
            icon = Icons.Default.LightMode,
            label = "Light Mode",
            isSelected = currentTheme == AppTheme.DEFAULT,
            onClick = { onThemeSelected(AppTheme.DEFAULT) }
        )
        ThemeOption(
            icon = Icons.Default.DarkMode,
            label = "Dark Mode",
            isSelected = currentTheme == AppTheme.DARK,
            onClick = { onThemeSelected(AppTheme.DARK) }
        )
        ThemeOption(
            icon = Icons.Default.Palette,
            label = "Green Theme",
            isSelected = currentTheme == AppTheme.GREEN,
            onClick = { onThemeSelected(AppTheme.GREEN) }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Divider(color = Color.Gray.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(24.dp))

        // Settings Logic
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable { /* Handle settings */ },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Text("Settings", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
    }
}

@Composable
fun ThemeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        if (isSelected) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentScreen == AppScreen.HOME,
            onClick = { onScreenSelected(AppScreen.HOME) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
            label = { Text("Calendar") },
            selected = currentScreen == AppScreen.CALENDAR,
            onClick = { onScreenSelected(AppScreen.CALENDAR) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
            label = { Text("Statistics") },
            selected = currentScreen == AppScreen.STATS,
            onClick = { onScreenSelected(AppScreen.STATS) }
        )
    }
}
