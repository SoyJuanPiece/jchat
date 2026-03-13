package com.jchat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jchat.domain.repository.IChatRepository
import kotlinx.coroutines.launch
import com.jchat.presentation.AuthScreen
import com.jchat.presentation.conversation.ConversationScreen
import com.jchat.presentation.profile.ProfileScreen
import com.jchat.presentation.home.HomeScreen
import com.jchat.presentation.settings.AboutScreen
import com.jchat.presentation.settings.BlockedUsersScreen
import com.jchat.presentation.settings.ChangePasswordScreen
import com.jchat.presentation.settings.ReportProblemScreen
import com.jchat.presentation.settings.SettingsScreen
import com.jchat.presentation.settings.ThemeOption
import org.koin.compose.koinInject
import androidx.compose.runtime.rememberCoroutineScope

private const val ROUTE_AUTH = "auth"
private const val ROUTE_HOME = "home"
private const val ROUTE_CONVERSATION = "conversation/{chatId}"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_CHANGE_PASSWORD = "settings/change-password"
private const val ROUTE_BLOCKED_USERS = "settings/blocked-users"
private const val ROUTE_ABOUT = "settings/about"
private const val ROUTE_REPORT_PROBLEM = "settings/report-problem"
private const val ARG_CHAT_ID = "chatId"

/**
 * Top-level navigation graph for JChat.
 */
@Composable
fun JChatNavGraph(
    currentUserId: String?,
    onThemeChanged: (ThemeOption) -> Unit = {},
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val repository = koinInject<IChatRepository>()
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
            HomeScreen(
                onNavigateToConversation = { chatId ->
                    navController.navigate("conversation/$chatId")
                },
                onNavigateToSettings = {
                    navController.navigate(ROUTE_SETTINGS)
                },
                onSignOut = {
                    scope.launch {
                        repository.signOut()
                    }
                }
            )
        }

        composable(ROUTE_PROFILE) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = {
                    navController.navigate(ROUTE_PROFILE)
                },
                onNavigateToChangePassword = {
                    navController.navigate(ROUTE_CHANGE_PASSWORD)
                },
                onNavigateToBlockedUsers = {
                    navController.navigate(ROUTE_BLOCKED_USERS)
                },
                onNavigateToAbout = {
                    navController.navigate(ROUTE_ABOUT)
                },
                onNavigateToReportProblem = {
                    navController.navigate(ROUTE_REPORT_PROBLEM)
                },
                onThemeChanged = onThemeChanged,
            )
        }

        composable(ROUTE_CHANGE_PASSWORD) {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(ROUTE_BLOCKED_USERS) {
            BlockedUsersScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(ROUTE_ABOUT) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(ROUTE_REPORT_PROBLEM) {
            ReportProblemScreen(
                onNavigateBack = { navController.popBackStack() },
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
