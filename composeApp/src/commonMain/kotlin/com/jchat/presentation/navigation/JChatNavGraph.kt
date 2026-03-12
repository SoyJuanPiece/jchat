package com.jchat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jchat.presentation.chatlist.ChatListScreen
import com.jchat.presentation.conversation.ConversationScreen

private const val ROUTE_CHAT_LIST = "chat_list"
private const val ROUTE_CONVERSATION = "conversation/{chatId}"
private const val ARG_CHAT_ID = "chatId"

/**
 * Top-level navigation graph for JChat.
 *
 * @param currentUserId The authenticated user's ID, required by [ConversationScreen]
 *                      to distinguish own messages from others.
 */
@Composable
fun JChatNavGraph(currentUserId: String) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_CHAT_LIST,
    ) {
        composable(ROUTE_CHAT_LIST) {
            ChatListScreen(
                onNavigateToConversation = { chatId ->
                    navController.navigate("conversation/$chatId")
                },
            )
        }

        composable(
            route = ROUTE_CONVERSATION,
            arguments = listOf(
                navArgument(ARG_CHAT_ID) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val chatId = requireNotNull(backStackEntry.arguments?.getString(ARG_CHAT_ID))
            ConversationScreen(
                chatId = chatId,
                currentUserId = currentUserId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
