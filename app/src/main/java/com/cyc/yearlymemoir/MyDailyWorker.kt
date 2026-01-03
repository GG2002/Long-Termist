package com.cyc.yearlymemoir

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cyc.yearlymemoir.MainActivity.Companion.appContext
import com.cyc.yearlymemoir.MainActivity.Companion.ds
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.model.UniversalMDDateType
import com.cyc.yearlymemoir.domain.repository.BalanceChannelType
import com.cyc.yearlymemoir.domain.repository.TimePeriod
import com.cyc.yearlymemoir.services.NotificationHelper
import com.cyc.yearlymemoir.services.OverlayConfirmManager
import com.cyc.yearlymemoir.services.RemindMeService
import com.cyc.yearlymemoir.venassists.GetBalanceStep
import com.cyc.yearlymemoir.venassists.GetTodayBalanceStepImpl
import com.ven.assists.AssistsCore
import com.ven.assists.stepper.StepManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GetAppBalanceWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

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


    suspend fun checkAndUpdateIfNeeded(context: Context): Boolean {
        val ds = MainActivity.ds // 假设 ds 是线程安全的
        val lastUpdateTime = ds.getDouble("last_update_time")

        // 核心逻辑：判断上次更新是否在今天之前
        if (isBeforeToday(lastUpdateTime.toLong())) {
            Log.d(
                "WorkScheduler",
                "Update required. Last update timestamp: $lastUpdateTime. Starting confirmation flow."
            )

            // 无限循环直到用户确认
            while (true) {
                val confirmed = OverlayConfirmManager.show(
                    context = context.applicationContext,
                    title = "余额识别",
                    message = "是否现在开始识别余额？"
                )

                if (confirmed) {
                    if (AssistsCore.isAccessibilityServiceEnabled()) {
                        val currentTimeSeconds = System.currentTimeMillis().toDouble()
                        ds.putDouble("last_update_time", currentTimeSeconds)
                        Log.d("WorkScheduler", "Timestamp updated to: $currentTimeSeconds")
                        break // 成功更新，跳出循环
                    }
                    AssistsCore.openAccessibilitySetting()
                }

                delay(20_000) // 等待20秒后重试
            }
            return true
        }

        Log.d("WorkScheduler", "Already updated today. No action needed.")
        return false
    }

    // doWork() 是后台线程执行的入口点
    override suspend fun doWork(): Result {
        Log.d("GetAppBalanceWorker", "Worker is running...")

        return withContext(Dispatchers.IO) {
            try {
                if (!checkAndUpdateIfNeeded(appContext)) {
                    return@withContext Result.success()
                }
                ds.resetTodayBalance(BalanceChannelType.ALL)
                StepManager.execute(
                    GetTodayBalanceStepImpl::class.java,
                    GetBalanceStep.RESTART_ZFB.ordinal,
                    0
                )

                Log.d("GetAppBalanceWorker", "Work finished successfully.")
                Result.success()
            } catch (e: Exception) {
                Log.e("GetAppBalanceWorker", "Work failed", e)
                Result.failure()
            }
        }
    }
}

class RemindMeWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    // doWork() 是后台线程执行的入口点
    override suspend fun doWork(): Result {
        Log.d("RemindMeWorker", "Worker is running...")

        return withContext(Dispatchers.IO) {
            try {
                val (details, _) = MainApplication.repository.getFirstDayFieldDataByName(
                    "提醒我",
                    TimePeriod.DAY
                )
                val today = UniversalDate.today()

                // 分类收集消息
                val persistentList = mutableListOf<String>()
                val simpleList = mutableListOf<String>()

                details.forEach {
                    val detailName = it.detail
                    val universalDate =
                        UniversalDate(it.year, UniversalMDDateType.fromString(it.mdDate)!!)
                    when (val diffDays = universalDate.diff(today, false)) {
                        0 -> {
                            persistentList.add("今天有 $detailName")
                        }

                        -1 -> {
                            persistentList.add("$detailName 还剩${-diffDays}天")
                        }

                        -3, -7 -> {
                            simpleList.add("$detailName 还剩${-diffDays}天")
                        }
                    }
                }

                // 显示普通通知
                if (simpleList.isNotEmpty()) {
                    val contentText = simpleList.joinToString("，")
                    NotificationHelper.showSimpleNotification(appContext, "提醒事项", contentText)
                }

                // 启动常驻通知服务
                if (persistentList.isNotEmpty()) {
                    val contentText = persistentList.joinToString("，")
                    println(contentText)
                    val remindMeIntent = Intent(appContext, RemindMeService::class.java).apply {
                        putExtra(RemindMeService.EXTRA_TITLE, "即将来临的重要日期")
                        putExtra(RemindMeService.EXTRA_TEXT, contentText)
                    }
                    appContext.startForegroundService(remindMeIntent)
                }

                Log.d("RemindMeWorker", "Work finished successfully.")
                Result.success()
            } catch (e: Exception) {
                Log.e("RemindMeWorker", "Work failed", e)
                Result.failure()
            }
        }
    }
}

object WorkScheduler {

    const val UNIQUE_WORK_NAME = "GetPersonalBalance8AMWork"
    private const val TEST_WORK_TAG = "MyTestWork"

    fun enableGetPersonalBalance8amWork(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 1. 创建一个周期为 8 小时的请求
        val dailyWorkRequest = PeriodicWorkRequestBuilder<GetAppBalanceWorker>(8, TimeUnit.HOURS)
            // 2. 计算从现在到下一个早上 8 点的延迟
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().build()) // 可以添加网络等约束
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )

        val initialDelayMinutes = TimeUnit.MILLISECONDS.toMinutes(calculateInitialDelay())
        Log.d(
            "WorkScheduler",
            "$UNIQUE_WORK_NAME work scheduled. First run in approx. $initialDelayMinutes minutes."
        )
    }

    fun cancelGetPersonalBalance8amWork(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
        Log.d(
            "WorkScheduler",
            "$UNIQUE_WORK_NAME work canceled."
        )
    }


    private fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 如果今天的8点已经过了，则将目标时间设置为明天的8点
        if (dueTime.before(currentTime)) {
            dueTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        return dueTime.timeInMillis - currentTime.timeInMillis
    }

    /**
     * 用于测试的立即执行任务。
     */
    fun scheduleNowForRemindMe(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val testWorkRequest = OneTimeWorkRequestBuilder<RemindMeWorker>()
            .addTag(TEST_WORK_TAG)
            .build()
        workManager.enqueue(testWorkRequest)
        Log.d("WorkScheduler", "One-time test work has been enqueued.")
    }
}