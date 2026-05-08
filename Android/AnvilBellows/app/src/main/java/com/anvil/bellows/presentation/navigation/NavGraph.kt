package com.anvil.bellows.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.anvil.bellows.presentation.ui.agents.AgentPresetEditorScreen
import com.anvil.bellows.presentation.ui.chat.ChatOverviewScreen
import com.anvil.bellows.presentation.ui.chat.ChatScreen
import com.anvil.bellows.presentation.ui.providers.ProvidersScreen
import com.anvil.bellows.presentation.ui.projects.ProjectsScreen
import com.anvil.bellows.presentation.ui.quota.QuotaDashboardScreen
import com.anvil.bellows.presentation.ui.settings.SettingsScreen
import com.anvil.bellows.presentation.viewmodel.ChatViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.AutoMirrored.Filled.Chat)
    object Providers : Screen("providers", "Providers", Icons.Default.Tune)
    object Projects : Screen("projects", "Projects", Icons.Default.Folder)
    object Quota : Screen("quota", "Quota", Icons.Default.BarChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavScreens = listOf(
    Screen.Chat, Screen.Providers, Screen.Projects, Screen.Quota, Screen.Settings
)

// Destination routes not in the bottom nav bar
private object Routes {
    const val CHAT_DETAIL = "chat/{presetId}"
    const val AGENT_EDITOR = "agents/edit/{presetId}"
    const val AGENT_NEW = "agents/edit/new"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    // Shared ChatViewModel so overview and chat screen share the same instance
    val chatViewModel: ChatViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavScreens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.Chat.route) {
            // Chat overview — agent/preset picker
            composable(Screen.Chat.route) {
                ChatOverviewScreen(
                    paddingValues = innerPadding,
                    onStartPreset = { preset ->
                        chatViewModel.startPresetChat(preset)
                        navController.navigate("chat/${preset.id}")
                    },
                    onEditPreset = { presetId ->
                        navController.navigate("agents/edit/$presetId")
                    },
                    onNewPreset = {
                        navController.navigate(Routes.AGENT_NEW)
                    }
                )
            }

            // Active chat conversation
            composable("chat/{presetId}") {
                ChatScreen(paddingValues = innerPadding, viewModel = chatViewModel)
            }

            // Agent preset editor (existing preset)
            composable("agents/edit/{presetId}") { backStackEntry ->
                val presetId = backStackEntry.arguments?.getString("presetId")
                AgentPresetEditorScreen(
                    presetId = presetId,
                    onBack = { navController.popBackStack() }
                )
            }

            // Agent preset editor (new preset)
            composable(Routes.AGENT_NEW) {
                AgentPresetEditorScreen(
                    presetId = null,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Providers.route) { ProvidersScreen(innerPadding) }
            composable(Screen.Projects.route) { ProjectsScreen(innerPadding) }
            composable(Screen.Quota.route) { QuotaDashboardScreen(innerPadding) }
            composable(Screen.Settings.route) { SettingsScreen(innerPadding) }
        }
    }
}
