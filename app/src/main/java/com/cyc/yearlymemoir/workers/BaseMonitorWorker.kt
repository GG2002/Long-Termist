package com.cyc.yearlymemoir.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cyc.yearlymemoir.data.local.db.DebugInfoDatabase
import com.cyc.yearlymemoir.data.local.entity.TaskExecutionLog
import com.cyc.yearlymemoir.utils.LogRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseMonitorWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    abstract suspend fun doActualWork(): Result

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = DebugInfoDatabase.get(applicationContext)
        val dao = db.taskExecutionLogDao()

        val workId = id.toString()
        val tag = tags.firstOrNull() ?: this@BaseMonitorWorker.javaClass.simpleName
        val inputJson = inputData.keyValueMap.toString()
        val scheduleTime = System.currentTimeMillis() // 若有计划时间可通过 inputData 注入，此处先用当前
        val startTime = System.currentTimeMillis()

        // STARTING
        val logId = dao.insert(
            TaskExecutionLog(
                work_uuid = workId,
                task_tag = tag,
                status = "STARTING",
                input_params = inputJson,
                output_result = null,
                trigger_type = null,
                schedule_time = scheduleTime,
                start_time = startTime,
                end_time = null
            )
        )

        // start log recording session
        LogRecorder.startSession(workId)

        return@withContext try {
            val result = doActualWork()
            val endTime = System.currentTimeMillis()
            // flush logs and store into output_result for SUCCESS
            val aggregatedLogs = LogRecorder.endSession()
            dao.updateResult(logId, "SUCCESS", aggregatedLogs, endTime)
            result
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            val aggregatedLogs = try {
                LogRecorder.endSession()
            } catch (_: Exception) {
                ""
            }
            val stack = e.stackTraceToString()
            val combined = buildString {
                appendLine("[Exception]")
                appendLine(stack)
                appendLine("[Logs]")
                append(aggregatedLogs)
            }
            dao.updateResult(logId, "FAILURE", combined, endTime)
            Result.failure()
        }
    }
}
