package com.cyc.yearlymemoir

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cyc.yearlymemoir.workers.GetAppBalanceWorker
import com.cyc.yearlymemoir.workers.RemindMeWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 统一任务定义
 */
data class TaskDefinition(
    val tag: String,
    val name: String,
    val description: String,
    val periodHours: Long? = null
)

/**
 * 统一调度器：合并原 TaskScheduler 与 DailyWorker 的能力。
 */
object WorkScheduler {

    // 任务唯一标识常量
    const val TAG_GET_APP_BALANCE = "GetAppBalanceWorker"
    const val TAG_REMIND_ME = "RemindMeWorker"

    // 唯一周期任务名称（用于 8 点对齐的余额任务）
    const val WORK_GET_BALANCE_8AM = "GetPersonalBalance8AMWork"

    // 任务注册表（供 UI 展示与调用）
    val registry = listOf(
        TaskDefinition(
            tag = TAG_GET_APP_BALANCE,
            name = "余额识别 - 一次性任务",
            description = "立即执行一次",
        ),
        TaskDefinition(
            tag = TAG_REMIND_ME,
            name = "提醒我 - 一次性任务",
            description = "立即生成提醒"
        )
    )

    /**
     * 通用：按小时周期排程（不做 8 点对齐）。
     */
    fun enqueuePeriodic(context: Context, tag: String, hours: Long) {
        val request = PeriodicWorkRequestBuilder<GetAppBalanceWorker>(hours, TimeUnit.HOURS)
            .addTag(tag)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private fun calculateInitialDelayTo8AM(): Long {
        val currentTime = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (dueTime.before(currentTime)) {
            dueTime.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dueTime.timeInMillis - currentTime.timeInMillis
    }

    /**
     * 余额任务：每 8 小时一次，但首轮与早上 8 点对齐。
     */
    fun enableGetPersonalBalance8amWork(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val dailyWorkRequest = PeriodicWorkRequestBuilder<GetAppBalanceWorker>(8, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelayTo8AM(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().build())
            .addTag(TAG_GET_APP_BALANCE)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_GET_BALANCE_8AM,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )

        val initialDelayMinutes = TimeUnit.MILLISECONDS.toMinutes(calculateInitialDelayTo8AM())
        Log.d(
            "WorkScheduler",
            "$WORK_GET_BALANCE_8AM scheduled. First run in approx. $initialDelayMinutes minutes."
        )
    }

    /**
     * 立即运行：根据 tag 选择对应 Worker。
     */
    fun runTaskNow(context: Context, tag: String) {
        val request = when (tag) {
            TAG_REMIND_ME -> OneTimeWorkRequestBuilder<RemindMeWorker>()
                .addTag(tag)
                .build()

            TAG_GET_APP_BALANCE -> OneTimeWorkRequestBuilder<GetAppBalanceWorker>()
                .addTag(tag)
                .build()

            else -> return
        }
        WorkManager.getInstance(context)
            .enqueueUniqueWork("${tag}_one_shot", ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * 统一取消接口：按 tag 取消（包含所有带该 tag 的任务）。
     */
    fun cancelByTag(context: Context, tag: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag)
        Log.d("WorkScheduler", "Cancelled all works by tag=$tag")
    }
}