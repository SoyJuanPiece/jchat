package com.jchat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import com.jchat.data.remote.RemoteDataSource
import com.jchat.presentation.navigation.JChatNavGraph
import org.koin.compose.koinInject

/**
 * Root Composable for JChat.
 *
 * Wrap everything in [MaterialTheme] so all child composables
 * automatically inherit the app's design tokens.
 */
@Composable
fun App() {
    val remote = koinInject<RemoteDataSource>()
    val currentUserId by remote.authSessionFlow.collectAsState(initial = remote.getCurrentUserId())

    MaterialTheme {
        Surface {
            JChatNavGraph(currentUserId = currentUserId)
        }
    }
}
