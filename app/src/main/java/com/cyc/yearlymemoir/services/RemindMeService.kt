package com.cyc.yearlymemoir.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.R

class RemindMeService : Service() {
    companion object {
        const val NOTIFICATION_ID = 1 // 常驻通知的 ID 不能为 0

        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_TEXT = "EXTRA_TEXT"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 创建通知
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "服务正在运行"
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: "点击以返回应用"
        val notification = NotificationHelper.createForegroundNotification(this, title, text)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // 服务停止时，可以在这里做一些清理工作
    }

    override fun onBind(intent: Intent?): IBinder? {
        // 我们不提供绑定，所以返回 null
        return null
    }
}

object NotificationHelper {
    // 显示一个普通的通知
    fun showSimpleNotification(context: Context, contextTitle: String, contentText: String) {
        // 检查权限
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 在实际应用中，你应该在 UI 层请求权限，这里只是一个安全检查
            return
        }

        // 当用户点击通知时，打开 MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, MainApplication.CHANNEL_ID_NORMAL)
            .setSmallIcon(R.drawable.app_icon) // 必须设置一个小图标
            .setContentTitle(contextTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // 设置点击意图
            .setAutoCancel(true) // 点击后自动取消通知

        // 使用一个唯一的 ID 来显示通知
        val notificationId = 1001
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    // 为前台服务创建一个通知（下一步会用到）
    fun createForegroundNotification(
        context: Context,
        contextTitle: String,
        contentText: String
    ): Notification {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, MainApplication.CHANNEL_ID_FOREGROUND)
            .setContentTitle(contextTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true) // 设置为常驻通知
            .build()
    }
}