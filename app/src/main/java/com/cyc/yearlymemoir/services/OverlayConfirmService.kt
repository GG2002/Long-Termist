package com.cyc.yearlymemoir.services

// File: OverlayConfirmService.kt

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class OverlayConfirmService : Service() {

    companion object {
        // 定义广播动作，用包名做前缀保证唯一性
        const val ACTION_CONFIRM_RESULT = "com.cyc.yearlymemoir.CONFIRM_RESULT"
        const val EXTRA_CONFIRMED = "extra_confirmed"
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    private val NOTIFICATION_ID = 101

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // 1. 创建一个通知
        val notification: Notification =
            NotificationCompat.Builder(this, MainApplication.CHANNEL_ID_OVERLAY)
                .setContentTitle("正在显示确认窗口")
                .setContentText("请在屏幕中央完成操作")
                .setSmallIcon(R.drawable.chi_avatar) // **必须**设置一个小图标！
                .setPriority(NotificationCompat.PRIORITY_LOW) // 降低通知的打扰程度
                .build()

        // 2. 将 Service 提升为前台服务
        // 这一步必须在启动后的几秒内完成！
        startForeground(NOTIFICATION_ID, notification)

        // 防止重复创建
        if (overlayView != null) {
            return START_NOT_STICKY
        }

        // 从 Intent 获取数据
        val title = intent?.getStringExtra("TITLE") ?: "确认"
        val message = intent?.getStringExtra("MESSAGE") ?: "请确认此操作"

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val themedContext = ContextThemeWrapper(
            this,
            R.style.AppTheme
        ) // 确保 R.style.Theme_MyApp 是你在 themes.xml 中定义的主题名
        val inflater = LayoutInflater.from(themedContext)

        overlayView = inflater.inflate(R.layout.overlay_confirm_view, null)

        // 3. 设置布局参数 (LayoutPa rams)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            // 这是关键！使用悬浮窗类型
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // 让窗口可聚焦，以便接收点击事件
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        // 4. 绑定视图和事件
        setupView(overlayView!!, title, message)

        // 5. 将视图添加到窗口
        windowManager.addView(overlayView, params)

        return START_NOT_STICKY
    }

    private fun setupView(view: View, title: String, message: String) {
        view.findViewById<TextView>(R.id.overlay_title).text = title
        view.findViewById<TextView>(R.id.overlay_message).text = message

        view.findViewById<Button>(R.id.overlay_confirm_button).setOnClickListener {
            sendResultAndStop(true)
        }

        view.findViewById<Button>(R.id.overlay_cancel_button).setOnClickListener {
            sendResultAndStop(false)
        }
    }

    private fun sendResultAndStop(confirmed: Boolean) {
        val intent = Intent(ACTION_CONFIRM_RESULT).apply {
            putExtra(EXTRA_CONFIRMED, confirmed)
        }
        sendBroadcast(intent)
        stopSelf() // 完成任务，关闭服务
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }

        // 停止前台服务，通知也会被移除
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}

object OverlayConfirmManager {
    // 用于连接 suspend 函数和 Service 回调的桥梁
    internal var onResult: ((Boolean) -> Unit)? = null

    /**
     * 显示一个悬浮确认框并等待用户操作结果。
     * @param context Context
     * @param title 弹窗标题
     * @param message 弹窗信息
     * @return true 如果用户点击确认，false 如果点击取消或关闭
     */
    suspend fun show(
        context: Context,
        title: String,
        message: String
    ): Boolean {
        // 1. 检查权限
        if (!hasOverlayPermission(context)) {
            requestOverlayPermission(context)
            return false
        }

        return suspendCancellableCoroutine { continuation ->
            // 1. 创建一个临时的广播接收器
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == OverlayConfirmService.ACTION_CONFIRM_RESULT) {
                        val result =
                            intent.getBooleanExtra(OverlayConfirmService.EXTRA_CONFIRMED, false)

                        // 确保协程还在活动状态，防止重复恢复
                        if (continuation.isActive) {
                            // 恢复协程，并把结果传回去！
                            continuation.resume(result)
                        }

                        // 注销接收器
                        context?.unregisterReceiver(this)
                    }
                }
            }

            // 协程被取消时的清理工作 (比如用户退出了这个界面)
            continuation.invokeOnCancellation {
                try {
                    context.unregisterReceiver(receiver)
                } catch (e: Exception) {
                    // 如果已经注销，可能会抛出异常，忽略即可
                }
            }

            // 2. 注册广播接收器，让它只监听我们定义好的动作
            val filter = IntentFilter(OverlayConfirmService.ACTION_CONFIRM_RESULT)
            // 从 Android 13 (Tiramisu) 开始，需要明确指定导出行为
            val flags =
                ContextCompat.RECEIVER_VISIBLE_TO_INSTANT_APPS
            ContextCompat.registerReceiver(context, receiver, filter, flags)


            // 3. 启动 Service
            val serviceIntent = Intent(context, OverlayConfirmService::class.java).apply {
                putExtra("TITLE", title)
                putExtra("MESSAGE", message)
            }

            context.startForegroundService(serviceIntent)
        }
    }

    /**
     * 检查是否拥有悬浮窗权限
     */
    private fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 请求悬浮窗权限
     */
    private fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        // 需要从 Activity context 启动
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}