package com.jchat.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jchat.presentation.calls.CallsScreen
import com.jchat.presentation.chatlist.ChatListIntent
import com.jchat.presentation.chatlist.ChatListScreen
import com.jchat.presentation.chatlist.ChatListViewModel
import com.jchat.presentation.updates.UpdatesScreen
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToConversation: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ChatListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    
    val tabs = listOf(
        HomeTab("Chats", Icons.Default.Chat),
        HomeTab("Updates", Icons.Default.Update),
        HomeTab("Communities", Icons.Default.Groups),
        HomeTab("Calls", Icons.Default.Call)
    )

    Scaffold(
        topBar = {
            if (state.isSearchMode) {
                SearchTopBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onIntent(ChatListIntent.UpdateSearchQuery(it)) },
                    onClose = { viewModel.onIntent(ChatListIntent.ToggleSearchMode(false)) }
                )
            } else {
                TopAppBar(
                    title = { Text("JChat") },
                    actions = {
                        IconButton(onClick = { /* Open Camera */ }) { Icon(Icons.Default.PhotoCamera, contentDescription = "Camera") }
                        IconButton(onClick = { viewModel.onIntent(ChatListIntent.ToggleSearchMode(true)) }) { 
                            Icon(Icons.Default.Search, contentDescription = "Search") 
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Profile") },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToProfile()
                                    },
                                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sign Out") },
                                    onClick = {
                                        showMenu = false
                                        onSignOut()
                                    },
                                    leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) }
                                )
                            }
                        }
                    }
                )
            }
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
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedTab) {
                0 -> ChatListScreen(onNavigateToConversation = onNavigateToConversation, viewModel = viewModel)
                1 -> UpdatesScreen()
                2 -> CenterText("Communities - Coming Soon")
                3 -> CallsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search chats...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Composable
fun CenterText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}

data class HomeTab(val title: String, val icon: ImageVector)
