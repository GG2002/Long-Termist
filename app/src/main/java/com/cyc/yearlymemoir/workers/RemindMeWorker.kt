package com.cyc.yearlymemoir.workers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkerParameters
import com.cyc.yearlymemoir.MainApplication
import com.cyc.yearlymemoir.MainActivity.Companion.appContext
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.repository.TimePeriod
import com.cyc.yearlymemoir.services.NotificationHelper
import com.cyc.yearlymemoir.services.RemindMeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemindMeWorker(appContext: Context, workerParams: WorkerParameters) :
    BaseMonitorWorker(appContext, workerParams) {

    // doWork() 是后台线程执行的入口点
    override suspend fun doActualWork(): Result {
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
                    val universalDate = it.mdDate
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
                    Log.d("RemindMeWorker", "启动常驻通知，通知内容：$contentText")
                    val remindMeIntent = Intent(appContext, RemindMeService::class.java).apply {
                        putExtra(RemindMeService.EXTRA_TITLE, "即将来临的重要日期")
                        putExtra(RemindMeService.EXTRA_TEXT, contentText)
                    }
                    appContext.startForegroundService(remindMeIntent)
                }
                // 为空就取消通知
                else {
//                    val cancelIntent = Intent(appContext, RemindMeService::class.java).apply {
//                        action = RemindMeService.
//                    }
//                    appContext.startForegroundService(cancelIntent)
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
