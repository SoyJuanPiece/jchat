package com.jchat

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.jchat.data.remote.RemoteDataSource
import com.jchat.presentation.navigation.JChatNavGraph
import com.jchat.presentation.theme.JChatTheme
import org.koin.compose.koinInject

/**
 * Root Composable for JChat.
 */
@Composable
fun App() {
    val remote = koinInject<RemoteDataSource>()
    val currentUserId: String? by remote.authSessionFlow.collectAsState(initial = remote.getCurrentUserId())

    JChatTheme {
        Surface {
            JChatNavGraph(currentUserId = currentUserId)
        }
    }
}
