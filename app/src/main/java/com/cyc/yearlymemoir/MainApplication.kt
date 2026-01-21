package com.cyc.yearlymemoir

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.cyc.yearlymemoir.data.local.db.AppDatabase
import com.cyc.yearlymemoir.data.repository.LocalYearlyMemoirRepository
import com.cyc.yearlymemoir.domain.repository.DatastoreInit
import com.cyc.yearlymemoir.domain.repository.PreferencesKeys.WX_ENABLED
import com.cyc.yearlymemoir.domain.repository.PreferencesKeys.ZFB_ENABLED
import com.cyc.yearlymemoir.domain.repository.YearlyMemoirRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainApplication : Application() {

    // 创建应用级的协程作用域
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 懒加载数据库实例
    val database by lazy { AppDatabase.getInstance(this, applicationScope) }

    // tidb cloud 客户端
    // val dbClient by lazy { DbClient() }

    companion object {
        const val CHANNEL_ID_NORMAL = "normal_channel"
        const val CHANNEL_ID_FOREGROUND = "foreground_service_channel"
        const val CHANNEL_ID_OVERLAY = "overlay_confirm_channel"

        lateinit var ds: DatastoreInit
        lateinit var repository: YearlyMemoirRepository
        lateinit var instance: MainApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeData()
        createNotificationChannel()
    }

    private fun initializeData() {
        val localRepo = LocalYearlyMemoirRepository(
            database.detailDao(),
            database.yearlyDetailDao(),
            database.fieldDao(),
            database.balanceDao(),
            database.transactionDao(),
        )
        ds = DatastoreInit(applicationContext)
        if (ds.getString(WX_ENABLED).isNullOrBlank()) {
            ds.putString(WX_ENABLED, "true")
        }
        if (ds.getString(ZFB_ENABLED).isNullOrBlank()) {
            ds.putString(ZFB_ENABLED, "true")
        }
        repository = localRepo

        // 初始化云端数据库，采取混合模式备份数据
        // val cloudRepo = TidbCloudYearlyMemoirRepositoryImpl(
        //     dbClient.detailDao,
        //     dbClient.fieldDao
        // )
        // repository = HybridYearlyMemoirRepository(
        //     localRepo, cloudRepo, ds
        // )
        // applicationScope.launch {
        //     try {
        //         val hybridRepo = repository as? HybridYearlyMemoirRepository
        //         hybridRepo?.initializeDatabaseIfEmpty()
        //     } catch (e: Exception) {
        //         Log.e("数据同步", "在 Application 中后台初始化数据失败：${e.message}")
        //     }
        // }
    }

    private fun createNotificationChannel() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 普通通知渠道
        val normalChannel = NotificationChannel(
            CHANNEL_ID_NORMAL,
            "普通通知", // Channel Name
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "用于显示普通的、非紧急的通知"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(normalChannel)

        // 前台服务（常驻）通知渠道
        val foregroundChannel = NotificationChannel(
            CHANNEL_ID_FOREGROUND,
            "常驻服务通知", // Channel Name
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "用于正在运行的后台服务"
        }
        notificationManager.createNotificationChannel(foregroundChannel)

        // 微信悬浮窗通知渠道
        val wxFloatChannel = NotificationChannel(
            CHANNEL_ID_OVERLAY,
            "悬浮窗服务",
            NotificationManager.IMPORTANCE_LOW // 使用 LOW，有提示音 (如果设置了)，但不会弹出。
        ).apply {
            description = "用于显示重要的确认悬浮窗"
        }
        notificationManager.createNotificationChannel(wxFloatChannel)
    }
}