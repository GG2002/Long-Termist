package com.cyc.yearlymemoir.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Semaphore

/**
 * 任务级别日志收集器（信号量排队版）：
 * - 使用 Semaphore(1) 实现排队。
 * - 允许跨线程调用（例如：线程 A startSession，线程 B endSession），适合协程或异步回调场景。
 */
object LogRecorder {
    // 信号量，1 表示只允许 1 个任务同时持有许可（排队）
    // true 表示“公平锁”，即先来的先执行，不会插队
    private val semaphore = Semaphore(1, true)

    @Volatile
    private var active: Boolean = false
    private var workId: String? = null
    private val buffer = StringBuilder()

    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /**
     * 开始会话。
     * 消耗一个许可。如果许可已被其他任务拿走，这里会阻塞等待，直到对方 release。
     */
    fun startSession(id: String) {
        // 1. 获取许可（acquire 会阻塞，直到有空位）
        semaphore.acquire()
        
        try {
            // 2. 拿到许可后，初始化
            active = true
            workId = id
            buffer.setLength(0) 
            buffer.append("[LogSession started] workId=").append(id).append('\n')
        } catch (e: Exception) {
            // 初始化失败归还许可
            semaphore.release()
            throw e
        }
    }

    /**
     * 结束会话。
     * 释放一个许可，唤醒下一个排队的任务。
     * 允许任何线程调用此方法来结束会话。
     */
    fun endSession(): String {
        // 简单的防呆检查：如果没有 active，说明可能没 start 就调了 end，或者重复调用 end
        // 注意：Semaphore 无法像 Lock 那样查询“当前线程是否持有锁”，所以这里主要靠业务逻辑保证
        if (!active) {
            return ""
        }

        try {
            buffer.append("[LogSession ended] workId=").append(workId).append('\n')
            val result = buffer.toString()
            
            // 清理状态
            active = false
            workId = null
            buffer.setLength(0)
            
            return result
        } finally {
            // 3. 归还许可。注意：Semaphore 允许 A 线程 acquire，B 线程 release
            // 只要这里执行了，下一个等待的任务就会立即开始
            semaphore.release()
        }
    }

    private fun ts(): String = timeFmt.format(Date())

    private fun append(level: String, tag: String, msg: String, tr: Throwable? = null) {
        if (active) {
            synchronized(buffer) { // 防止多线程同时 append 导致 StringBuilder 内部错乱
                buffer.append(ts())
                    .append(" ")
                    .append(level)
                    .append("/")
                    .append(tag)
                    .append(": ")
                    .append(msg)
                    .append('\n')
                if (tr != null) {
                    buffer.append(tr.stackTraceToString()).append('\n')
                }
            }
        }
    }

    // Mirror android.util.Log APIs
    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        append("D", tag, msg)
    }

    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
        append("I", tag, msg)
    }

    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
        append("W", tag, msg)
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) Log.e(tag, msg, tr) else Log.e(tag, msg)
        append("E", tag, msg, tr)
    }
}