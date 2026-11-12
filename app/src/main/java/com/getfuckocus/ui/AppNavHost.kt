package com.getfuckocus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.getfuckocus.container
import com.getfuckocus.data.VerifyMethod
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object Routes {
    const val TASKS = "tasks"
    const val SETTINGS = "settings"
    const val ALLOWED_APPS = "allowed_apps"
    const val ADD_TAG = "add_tag"
    const val EDIT = "edit/{taskId}"
    const val CAPTURE = "capture/{taskId}"
    const val CHECK_IN = "check/{taskId}"

    fun edit(taskId: Long) = "edit/$taskId"
    fun capture(taskId: Long) = "capture/$taskId"
    fun checkIn(taskId: Long) = "check/$taskId"
}

private val taskIdArgument = listOf(navArgument("taskId") { type = NavType.LongType })

@Composable
fun AppNavHost(startVerifyTaskId: Long?) {
    val nav = rememberNavController()
    val dao = LocalContext.current.container.taskDao

    // 잠금·억제 화면이나 알림에서 들어오면 그 과제의 인증 방식에 맞는 화면을 바로 연다
    LaunchedEffect(startVerifyTaskId) {
        val task = startVerifyTaskId?.let { dao.get(it) } ?: return@LaunchedEffect
        nav.navigate(if (task.verifyMethod == VerifyMethod.PHOTO) Routes.capture(task.id) else Routes.checkIn(task.id))
    }

    NavHost(navController = nav, startDestination = Routes.TASKS) {
        composable(Routes.TASKS) {
            HomePager(
                onAdd = { nav.navigate(Routes.edit(0)) },
                onEdit = { nav.navigate(Routes.edit(it)) },
                onCapture = { nav.navigate(Routes.capture(it)) },
                onCheckScreen = { nav.navigate(Routes.checkIn(it)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onAddTag = { nav.navigate(Routes.ADD_TAG) },
            )
        }
        composable(Routes.CHECK_IN, arguments = taskIdArgument) { entry ->
            CheckInScreen(
                taskId = entry.arguments?.getLong("taskId") ?: 0L,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.ADD_TAG) {
            NfcTagAddScreen(onDone = { nav.popBackStack() })
        }
        composable(Routes.EDIT, arguments = taskIdArgument) { entry ->
            TaskEditScreen(
                taskId = entry.arguments?.getLong("taskId") ?: 0L,
                onDone = { nav.popBackStack() },
                onAddTag = { nav.navigate(Routes.ADD_TAG) },
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
