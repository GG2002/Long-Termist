package com.cyc.yearlymemoir.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.domain.repository.BalanceChannelType
import com.cyc.yearlymemoir.services.OverlayConfirmManager
import com.cyc.yearlymemoir.utils.LogRecorder
import com.cyc.yearlymemoir.venassists.GetBalanceStep
import com.cyc.yearlymemoir.venassists.GetTodayBalanceStepImpl
import com.ven.assists.AssistsCore
import com.ven.assists.stepper.StepManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GetAppBalanceWorker(appContext: Context, workerParams: WorkerParameters) :
    BaseMonitorWorker(appContext, workerParams) {
    /**
     * 辅助函数：检查给定的时间戳（秒）是否在今天零点之前。
     */
    private fun isBeforeToday(timestampMill: Long): Boolean {
        if (timestampMill <= 0) {
            return true // 从未更新过
        }

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMillis = todayStart.timeInMillis

        return timestampMill < todayStartMillis
    }

    /**
     * 格式化 Long 毫秒时间戳为 "yyyy-MM-dd HH:mm"。
     */
    private fun formatTs(ts: Long): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(ts)
        } catch (e: Exception) {
            ts.toString()
        }
    }


    suspend fun checkAndUpdateIfNeeded(context: Context): Boolean {
        val ds = MainApplication.ds
        val lastUpdateTime = ds.getLong("last_update_time")

        // 核心逻辑：判断上次更新是否在今天之前
        if (isBeforeToday(lastUpdateTime)) {
            LogRecorder.d(
                "WorkScheduler",
                "Update required. Last update timestamp: $lastUpdateTime. Starting confirmation flow."
            )

            // 无限循环直到用户确认
            var attempt = 0
            val fastTries = 6 // 前 6 次快速尝试
            while (true) {
                // 阶段 1：未显示 -> 无限 10 秒重试直到显示成功
                val shown = OverlayConfirmManager.ensureShown(
                    context = context.applicationContext,
                    title = "余额识别",
                    message = "上次更新时间：${formatTs(lastUpdateTime)}\n是否现在开始识别余额？"
                )
                if (!shown) {
                    LogRecorder.i(
                        "WorkScheduler",
                        "Overlay not shown yet. Retrying in 10 seconds..."
                    )
                    delay(10_000)
                    continue
                }

                // 阶段 2：已显示 -> 应用重试次数与退避时长策略，等待用户结果
                attempt++
                val confirmed = OverlayConfirmManager.waitForResult(context.applicationContext)

                if (confirmed) {
                    if (AssistsCore.isAccessibilityServiceEnabled()) {
                        break // 成功更新，跳出阶段 2 并结束整个流程
                    }
                    // 引导开启辅助功能后，进入退避等待再重试
                    AssistsCore.openAccessibilitySetting()
                }

                // 阶段 2 退避：前几次 20s，之后 60s
                val delayMs = if (attempt <= fastTries) 20_000L else 60_000L
                LogRecorder.i(
                    "WorkScheduler",
                    "User not confirmed yet. Attempt #$attempt. Retrying in ${delayMs / 1000} seconds..."
                )
                delay(delayMs)
            }
            return true
        }

        LogRecorder.d("WorkScheduler", "Already updated today. No action needed.")
        return false
    }

    // doWork() 是后台线程执行的入口点
    override suspend fun doActualWork(): Result {
        LogRecorder.d("GetAppBalanceWorker", "Worker is running...")

        return withContext(Dispatchers.IO) {
            try {
                if (!checkAndUpdateIfNeeded(applicationContext)) {
                    return@withContext Result.success()
                }
                MainApplication.ds.resetTodayBalance(BalanceChannelType.ALL)

                val deferred = CompletableDeferred<String>()

                // 1. 开始执行异步任务
                StepManager.execute(
                    GetTodayBalanceStepImpl::class.java,
                    GetBalanceStep.RESTART_ZFB.ordinal,
                    0, data = deferred
                )

                // 2. 在这里“暂停”，等待 StepManager 完成并填充 deferred
                // 如果 StepManager 内部发生错误并调用 deferred.completeExceptionally，这里会抛出异常
                val resultString = deferred.await()

                // 3. 拿到结果后继续执行
                LogRecorder.i("自动化完成", "Result: $resultString")
                LogRecorder.d("GetAppBalanceWorker", "Work finished successfully.")

                // 4. 返回 WorkManager 的结果
                Result.success()

            } catch (e: Exception) {
                // 如果 await 抛出异常（即任务失败），会进入这里
                LogRecorder.e("GetAppBalanceWorker", "Work failed or Exception occurred", e)
                Result.failure()
            }
        }
    }
}
