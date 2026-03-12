package com.jchat.presentation.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jchat.presentation.calls.CallsScreen
import com.jchat.presentation.chatlist.ChatListScreen
import com.jchat.presentation.updates.UpdatesScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToConversation: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    val tabs = listOf(
        HomeTab("Chats", Icons.Default.Chat),
        HomeTab("Updates", Icons.Default.Update),
        HomeTab("Communities", Icons.Default.Groups),
        HomeTab("Calls", Icons.Default.Call)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JChat") },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.PhotoCamera, contentDescription = "Camera") }
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    IconButton(onClick = onNavigateToProfile) { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* New Chat/Call logic */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    when(selectedTab) {
                        0 -> Icons.Default.Chat
                        1 -> Icons.Default.PhotoCamera
                        3 -> Icons.Default.Call
                        else -> Icons.Default.Chat
                    },
                    contentDescription = "Action"
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> ChatListScreen(
                    onNavigateToConversation = onNavigateToConversation,
                    onNavigateToProfile = onNavigateToProfile
                )
                1 -> UpdatesScreen()
                2 -> CenterText("Communities - Coming Soon")
                3 -> CallsScreen()
            }
        }
    }
}

@Composable
fun CenterText(text: String) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text)
    }
}

data class HomeTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
