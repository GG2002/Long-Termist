package com.cyc.yearlymemoir

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import com.cyc.yearlymemoir.domain.repository.DatastoreInit
import com.cyc.yearlymemoir.ui.PermissionScreen
import com.cyc.yearlymemoir.ui.personalbanlance.PersonalBalanceScreen
import com.cyc.yearlymemoir.ui.theme.YearlyMemoirTheme
import com.cyc.yearlymemoir.ui.yearlycalendar.CalendarViewModel
import com.cyc.yearlymemoir.ui.yearlycalendar.EveryDayScreen
import com.cyc.yearlymemoir.ui.yearlycalendar.YearlyCalendar


class MainActivity : ComponentActivity() {
    companion object {
        lateinit var appContext: Context
        lateinit var metrics: WindowMetrics
        lateinit var ds: DatastoreInit
        lateinit var navController: NavHostController
    }


    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContext = applicationContext
        metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        ds = DatastoreInit(appContext)


        val initialRoute = when (intent.action) {
            "com.cyc.yearlymemoir.PersonalBalanceScreen" -> "PersonalBalanceScreen"
            else -> "EveryDayScreen"
        }
//        val initialRoute = "PersonalBalanceScreen"
        val calendarViewModel = CalendarViewModel()

        WorkScheduler.scheduleNowForRemindMe(applicationContext)

        setContent {
            navController = rememberNavController()

            YearlyMemoirTheme {
                NavHost(
                    navController = navController, startDestination = initialRoute
                ) {
                    composable("PersonalBalanceScreen") {
                        PersonalBalanceScreen()
                    }
                    composable("EveryDayScreen") {
                        EveryDayScreen(
                            navController,
                        )
                    }
                    composable("YearlyCalendar") { YearlyCalendar(calendarViewModel) }
                    composable("PermissionScreen") { PermissionScreen() }
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
