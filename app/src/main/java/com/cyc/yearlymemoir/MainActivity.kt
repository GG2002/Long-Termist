package com.cyc.yearlymemoir

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.core.util.Consumer
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import com.cyc.yearlymemoir.domain.repository.DatastoreInit
import com.cyc.yearlymemoir.ui.personalbanlance.PersonalBalanceScreen
import com.cyc.yearlymemoir.ui.settings.PermissionScreen
import com.cyc.yearlymemoir.ui.settings.TaskControlPanelScreen
import com.cyc.yearlymemoir.ui.settings.TaskHistoryScreen
import com.cyc.yearlymemoir.ui.theme.YearlyMemoirTheme
import com.cyc.yearlymemoir.ui.yearlycalendar.DetailsManagementScreen
import com.cyc.yearlymemoir.ui.yearlycalendar.EveryDayScreen
import com.cyc.yearlymemoir.ui.yearlycalendar.YearlyCalendar


class MainActivity : ComponentActivity() {
    companion object {
        lateinit var appContext: Context
        lateinit var metrics: WindowMetrics
        lateinit var ds: DatastoreInit
        lateinit var navController: NavHostController
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContext = applicationContext
        metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        ds = DatastoreInit(appContext)


        WorkScheduler.runTaskNow(applicationContext, WorkScheduler.TAG_REMIND_ME)

        setContent {
            navController = rememberNavController()

            val startRoute = "EveryDayScreen"
            // val startRoute = "PersonalBalanceScreen"
            // val startRoute = "YearlyCalendar"
            // val startRoute = "TaskControlPanel"

            // App 冷启动，处理初始 Intent 跳转
            LaunchedEffect(Unit) {
                if (intent.action == "com.cyc.yearlymemoir.PersonalBalanceScreen") {
                    navController.navigate("PersonalBalanceScreen")
                }
            }
            // App 已经在后台，再次被唤起时，处理 Intent 跳转
            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent ->
                    if (newIntent.action == "com.cyc.yearlymemoir.PersonalBalanceScreen") {
                        // 关键点：避免重复入栈，先 navigate 再确保单一实例
                        navController.navigate("PersonalBalanceScreen") {
                            // 这里的逻辑可以防止多次点击导致堆叠多个 PersonalBalanceScreen
                            popUpTo("EveryDayScreen") {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }

            YearlyMemoirTheme {
                NavHost(
                    navController = navController, startDestination = startRoute
                ) {
                    composable("EveryDayScreen") { EveryDayScreen() }
                    composable("PermissionScreen") { PermissionScreen() }
                    composable("YearlyCalendar") { YearlyCalendar() }
                    composable("PersonalBalanceScreen") { PersonalBalanceScreen() }
                    composable("DetailsManagement") { DetailsManagementScreen() }
                    composable("TaskControlPanel") { TaskControlPanelScreen() }
                    composable("TaskHistory/{tag}") { backStack ->
                        val tag = backStack.arguments?.getString("tag") ?: ""
                        TaskHistoryScreen(tag)
                    }
                }
            }
        }


//        // 普通通知
//        NotificationHelper.showSimpleNotification(appContext, "普通通知", "可以划掉")
//
//        // 常驻通知
//        val remindMeIntent = Intent(appContext, RemindMeService::class.java).apply {
//            putExtra(RemindMeService.EXTRA_TITLE, "自定义常驻通知")
//            putExtra(RemindMeService.EXTRA_TEXT, "自定义文字")
//        }
//        appContext.startForegroundService(remindMeIntent)
    }
}
