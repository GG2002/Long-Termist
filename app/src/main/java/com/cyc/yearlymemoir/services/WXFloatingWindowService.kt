package com.cyc.yearlymemoir.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout // 使用 FrameLayout 可以更容易扩展

class WXFloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    companion object {
        const val KEY_RECT = "key_rect"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 初始化 LayoutParams，具体值将在 onStartCommand 中设置
        params = WindowManager.LayoutParams(
            0, // width
            0, // height
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(KEY_RECT, Rect::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(KEY_RECT)
        } ?: Rect(100, 200, 500, 400) // 默认值

        // 获取状态栏高度
        val statusBarHeight = getStatusBarHeight(this)
        // 更新 LayoutParams
        params.x = rect.left
        params.y = rect.top - statusBarHeight
        params.width = rect.width()
        params.height = rect.height()
        println("WXFloating params: $params")

        if (floatingView == null) {
            // --- 核心改动在这里 ---
            // 1. 创建一个简单的 View (这里用 FrameLayout，你也可以用 View)
            floatingView = FrameLayout(this)

            // 2. 设置它的背景颜色 (一个半透明的红色)
            floatingView?.setBackgroundColor(Color.parseColor("#80FF0000"))
            // 或者使用十六进制: floatingView?.setBackgroundColor(0x80FF0000.toInt())

            windowManager.addView(floatingView, params)
        } else {
            windowManager.updateViewLayout(floatingView, params)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let {
            windowManager.removeView(it)
        }
    }

    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        // 寻找系统资源中名为 "status_bar_height" 的尺寸资源
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}