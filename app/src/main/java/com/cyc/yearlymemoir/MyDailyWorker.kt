package com.cyc.yearlymemoir

import android.content.Context
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
import com.cyc.yearlymemoir.model.BalanceChannelType
import com.cyc.yearlymemoir.venassists.GetBalanceStep
import com.cyc.yearlymemoir.venassists.GetTodayBalanceStepImpl
import com.cyc.yearlymemoir.venassists.OverlayConfirmManager
import com.ven.assists.stepper.StepManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MyDailyWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    // doWork() 是后台线程执行的入口点
    override suspend fun doWork(): Result {
        Log.d("MyDailyWorker", "Worker is running...")

        return withContext(Dispatchers.IO) {
            try {
                if (!WorkScheduler.checkAndUpdateIfNeeded(appContext)) {
                    return@withContext Result.success()
                }

                ds.resetTodayBalance(BalanceChannelType.ALL)
                StepManager.execute(
                    GetTodayBalanceStepImpl::class.java,
                    GetBalanceStep.RESTART_ZFB.ordinal,
                    0
                )

                Log.d("MyDailyWorker", "Work finished successfully.")
                Result.success()
            } catch (e: Exception) {
                Log.e("MyDailyWorker", "Work failed", e)
                Result.failure()
            }
        }
    }
}

object WorkScheduler {

    private const val UNIQUE_WORK_NAME = "MyDaily8AMWork"
    private const val TEST_WORK_TAG = "MyTestWork"

    /**
     * 调度一个每天早上8点执行的周期性任务。
     * 这个方法应该在应用启动时调用一次，例如在 Application.onCreate() 中。
     * 使用 enqueueUniquePeriodicWork 可以防止重复调度。
     */
    fun scheduleDaily8amWork(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 1. 创建一个周期为24小时的请求
        val dailyWorkRequest = PeriodicWorkRequestBuilder<MyDailyWorker>(24, TimeUnit.HOURS)
            // 2. 计算从现在到下一个早上8点的延迟
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().build()) // 可以添加网络等约束
            .build()

        // 3. 使用唯一的名称来入队，保证任务的唯一性。
        // ExistingPeriodicWorkPolicy.KEEP: 如果已存在同名任务，则保留现有任务，不创建新的。
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )

        val initialDelayMinutes = TimeUnit.MILLISECONDS.toMinutes(calculateInitialDelay())
        Log.d(
            "WorkScheduler",
            "Daily work scheduled. First run in approx. $initialDelayMinutes minutes."
        )
    }

    /**
     * 检查是否需要更新，并处理用户确认流程。
     * 这个函数现在由 MyDailyWorker 在后台线程中调用。
     * 它内置了防止同一天重复执行的逻辑。
     */
    suspend fun checkAndUpdateIfNeeded(context: Context): Boolean {
        // 注意：直接从 Activity 引用静态变量 (MainActivity.ds) 不是一个好的实践，
        // 因为 Worker 的生命周期和 Activity 无关。
        // 建议使用依赖注入或一个单例的 Repository/DataStore 来获取数据源。
        // 这里为了保持和你代码一致，我们继续使用它。
        val ds = MainActivity.ds // 假设 ds 是线程安全的
        val lastUpdateTime = ds.getInt("last_update_time", 0)

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
                    Log.d("WorkScheduler", "User confirmed. Updating database and exiting loop.")
                    val currentTimeSeconds = (System.currentTimeMillis() / 1000).toInt()
                    ds.putInt("last_update_time", currentTimeSeconds)
                    Log.d("WorkScheduler", "Timestamp updated to: $currentTimeSeconds")
                    break // 成功更新，跳出循环
                } else {
                    Log.d("WorkScheduler", "User cancelled. Retrying after 20 seconds.")
                    delay(20_000) // 等待20秒后重试
                }
            }
            return true
        }

        Log.d("WorkScheduler", "Already updated today. No action needed.")
        return false
    }

    /**
     * 计算从当前时间到下一个早上8点的延迟时间（毫秒）。
     */
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
     * 辅助函数：检查给定的时间戳（秒）是否在今天零点之前。
     */
    private fun isBeforeToday(timestampSeconds: Long): Boolean {
        if (timestampSeconds <= 0) {
            return true // 从未更新过
        }

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMillis = todayStart.timeInMillis
        val lastUpdateMillis = timestampSeconds * 1000

        return lastUpdateMillis < todayStartMillis
    }

    /**
     * 用于测试的立即执行任务。
     */
    fun scheduleNowForTest(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val testWorkRequest = OneTimeWorkRequestBuilder<MyDailyWorker>()
            .addTag(TEST_WORK_TAG)
            .build()
        workManager.enqueue(testWorkRequest)
        Log.d("WorkScheduler", "One-time test work has been enqueued.")
    }
}