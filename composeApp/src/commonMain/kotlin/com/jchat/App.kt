package com.jchat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jchat.presentation.navigation.JChatNavGraph

/**
 * Root Composable for JChat.
 *
 * Wrap everything in [MaterialTheme] so all child composables
 * automatically inherit the app's design tokens.
 */
@Composable
fun App() {
    MaterialTheme {
        Surface {
            // In a real app, `currentUserId` would come from an auth ViewModel / session store.
            val currentUserId by remember { mutableStateOf("") }
            JChatNavGraph(currentUserId = currentUserId)
        }
    }
}
