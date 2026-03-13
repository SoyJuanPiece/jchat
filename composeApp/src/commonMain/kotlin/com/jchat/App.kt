package com.jchat

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.jchat.data.remote.RemoteDataSource
import com.jchat.presentation.navigation.JChatNavGraph
import com.jchat.presentation.settings.ThemeOption
import com.jchat.presentation.theme.JChatTheme
import org.koin.compose.koinInject

/**
 * Root Composable for JChat.
 */
@Composable
fun App() {
    val remote = koinInject<RemoteDataSource>()
    val currentUserId: String? by remote.authSessionFlow.collectAsState(initial = remote.getCurrentUserId())
    var themeOption by remember { mutableStateOf(ThemeOption.System) }

    JChatTheme(themeOption = themeOption) {
        Surface {
            JChatNavGraph(
                currentUserId = currentUserId,
                onThemeChanged = { themeOption = it },
            )
        }
    }
}
