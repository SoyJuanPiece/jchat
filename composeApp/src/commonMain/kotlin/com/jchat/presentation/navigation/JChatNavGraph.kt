package com.jchat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.jchat.presentation.AuthScreen
import com.jchat.presentation.conversation.ConversationScreen
import com.jchat.presentation.profile.ProfileScreen
import com.jchat.presentation.home.HomeScreen

private const val ROUTE_AUTH = "auth"
private const val ROUTE_HOME = "home"
private const val ROUTE_CONVERSATION = "conversation/{chatId}"
private const val ROUTE_PROFILE = "profile"
private const val ARG_CHAT_ID = "chatId"

/**
 * Top-level navigation graph for JChat.
 */
@Composable
fun JChatNavGraph(currentUserId: String?) {
    val navController = rememberNavController()
    val isAuthenticated = !currentUserId.isNullOrBlank()

    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) ROUTE_HOME else ROUTE_AUTH,
    ) {
        composable(ROUTE_AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(ROUTE_AUTH) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_HOME) {
            val remote = org.koin.compose.koinInject<com.jchat.data.remote.RemoteDataSource>()
            HomeScreen(
                onNavigateToConversation = { chatId ->
                    navController.navigate("conversation/$chatId")
                },
                onNavigateToProfile = {
                    navController.navigate(ROUTE_PROFILE)
                },
                onSignOut = {
                    kotlinx.coroutines.MainScope().launch {
                        remote.signOut()
                    }
                }
            )
        }

        composable(ROUTE_PROFILE) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
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
                currentUserId = currentUserId ?: "",
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
