package com.cyc.yearlymemoir

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import com.cyc.yearlymemoir.model.BalanceChannelType
import com.cyc.yearlymemoir.model.DatastoreInit
import com.cyc.yearlymemoir.ui.personalbanlance.PersonalBalanceScreen
import com.cyc.yearlymemoir.ui.theme.YearlyMemoirTheme
import com.cyc.yearlymemoir.ui.yearlycalendar.CalendarViewModel
import com.cyc.yearlymemoir.ui.yearlycalendar.EveryDayScreen
import com.cyc.yearlymemoir.ui.yearlycalendar.YearlyCalendar
import com.cyc.yearlymemoir.utils.UniversalDate
import com.cyc.yearlymemoir.utils.UniversalMDDateType
import com.ven.assists.AssistsCore
import java.time.LocalDate


class MainActivity : ComponentActivity() {
    companion object {
        lateinit var appContext: Context
        lateinit var metrics: WindowMetrics
        lateinit var ds: DatastoreInit
        const val OVERLAY_CHANNEL_ID = "overlay_confirm_channel"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 权限被授予
            } else {
                // 权限被拒绝
            }
        }

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContext = applicationContext
        metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        ds = DatastoreInit(appContext)
        createNotificationChannel()
        WorkScheduler.scheduleDaily8amWork(this)

        // 通知权限
        if (ContextCompat.checkSelfPermission(
                this,
                "android.permission.POST_NOTIFICATIONS"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
        }


        val initialRoute = when (intent?.action) {
            "com.cyc.yearlymemoir.PersonalBalanceScreen" -> "PersonalBalanceScreen"
            else -> "EveryDayScreen"
        }
//        val initialRoute = "PersonalBalanceScreen"
        val calendarViewModel = CalendarViewModel()

        setContent {
            val navController = rememberNavController()
            var universalDate by remember {
                mutableStateOf(
                    UniversalDate(
                        LocalDate.now().year,
                        UniversalMDDateType.MonthDay(
                            LocalDate.now().month.value,
                            LocalDate.now().dayOfMonth
                        )
                    )
                )
            }

            YearlyMemoirTheme {
                SharedTransitionLayout {
                    NavHost(
                        navController = navController, startDestination = initialRoute
                    ) {
                        composable("PersonalBalanceScreen") {
                            PersonalBalanceScreen(
                                this,
                                this@SharedTransitionLayout,
                                navController
                            )
                        }
                        composable("EveryDayScreen") {
                            EveryDayScreen(
                                this,
                                this@SharedTransitionLayout,
                                navController,
                            )
                        }
                        composable("YearlyCalendar") { YearlyCalendar(calendarViewModel) }
                    }
                }
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(16.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    UniversalDatePicker(
//                        initialDate = universalDate,
//                        onDateChanged = { newDate -> universalDate = newDate }
//                    )
//                }
            }
        }
    }

    private fun tmp() {
        println("全部：" + ds.getTodayBalance(BalanceChannelType.ALL))

        Log.i("权限检查-电池优化", "是否白名单：" + isIgnoringBatteryOptimizations())
        if (!isIgnoringBatteryOptimizations()) {
            requestIgnoreBatteryOptimizations()
        }
        Log.i("权限检查-无障碍", "是否开启：" + AssistsCore.isAccessibilityServiceEnabled())
        if (!AssistsCore.isAccessibilityServiceEnabled()) {
            AssistsCore.openAccessibilitySetting()
        }

    }

    private fun createNotificationChannel() {
        // 只在 Android 8.0 (API 26) 及以上版本创建渠道
        val name = "悬浮窗服务"
        val descriptionText = "用于显示重要的确认悬浮窗"
        val importance = NotificationManager.IMPORTANCE_LOW // 使用 LOW，避免声音和振动
        val channel = NotificationChannel(OVERLAY_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // 注册渠道
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("ServiceCast")
    private fun isIgnoringBatteryOptimizations(): Boolean {
        var isIgnoring = false
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        isIgnoring = powerManager.isIgnoringBatteryOptimizations(packageName)
        return isIgnoring
    }

    //添加到白名单
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations() {
        try {
            val intent: Intent =
                Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.setData(Uri.parse("package:$packageName"))
            startActivity(intent)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}
