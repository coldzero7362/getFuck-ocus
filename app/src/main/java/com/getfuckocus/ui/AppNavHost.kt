package com.getfuckocus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object Routes {
    const val TASKS = "tasks"
    const val SETTINGS = "settings"
    const val ALLOWED_APPS = "allowed_apps"
    const val EDIT = "edit/{taskId}"
    const val CAPTURE = "capture/{taskId}"

    fun edit(taskId: Long) = "edit/$taskId"
    fun capture(taskId: Long) = "capture/$taskId"
}

private val taskIdArgument = listOf(navArgument("taskId") { type = NavType.LongType })

@Composable
fun AppNavHost(startCaptureTaskId: Long?) {
    val nav = rememberNavController()

    LaunchedEffect(startCaptureTaskId) {
        startCaptureTaskId?.let { nav.navigate(Routes.capture(it)) }
    }

    NavHost(navController = nav, startDestination = Routes.TASKS) {
        composable(Routes.TASKS) {
            TaskListScreen(
                onAdd = { nav.navigate(Routes.edit(0)) },
                onEdit = { nav.navigate(Routes.edit(it)) },
                onCapture = { nav.navigate(Routes.capture(it)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.EDIT, arguments = taskIdArgument) { entry ->
            TaskEditScreen(
                taskId = entry.arguments?.getLong("taskId") ?: 0L,
                onDone = { nav.popBackStack() },
            )
        }
        composable(Routes.CAPTURE, arguments = taskIdArgument) { entry ->
            CaptureScreen(
                taskId = entry.arguments?.getLong("taskId") ?: 0L,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenAllowedApps = { nav.navigate(Routes.ALLOWED_APPS) },
            )
        }
        composable(Routes.ALLOWED_APPS) {
            AllowedAppsScreen(onBack = { nav.popBackStack() })
        }
    }
}
