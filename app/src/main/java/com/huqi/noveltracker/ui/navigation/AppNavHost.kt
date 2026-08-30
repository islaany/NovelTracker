package com.huqi.noveltracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.huqi.noveltracker.ui.screen.add.AddNovelScreen
import com.huqi.noveltracker.ui.screen.backup.BackupScreen
import com.huqi.noveltracker.ui.screen.detail.DetailScreen
import com.huqi.noveltracker.ui.screen.home.HomeScreen
import com.huqi.noveltracker.ui.screen.settings.SettingsScreen
import com.huqi.noveltracker.ui.screen.tags.TagsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Add : Screen("add")
    object Tags : Screen("tags")
    object Backup : Screen("backup")
    object Settings : Screen("settings")
    object Detail : Screen("detail/{novelId}") {
        fun createRoute(novelId: Long) = "detail/$novelId"
    }
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Add.route) {
            AddNovelScreen(navController = navController)
        }
        composable(Screen.Tags.route) {
            TagsScreen(navController = navController)
        }
        composable(Screen.Backup.route) {
            BackupScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("novelId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("novelId") ?: 0L
            DetailScreen(navController = navController, novelId = id)
        }
    }
}
