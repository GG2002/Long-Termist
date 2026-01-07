package com.cyc.yearlymemoir.venassists

import android.content.Intent
import com.cyc.yearlymemoir.utils.LogRecorder
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.ven.assists.stepper.StepCollector
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

abstract class BaseBalanceFlow(protected val flowCtx: FlowContext) {

    companion object {
        val recognizer by lazy {
            TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        }
    }

    // 子类必须实现这个注册方法
    abstract fun register(collector: StepCollector, nextStepId: Int)

    // --- 公共工具方法 ---

    protected fun restartApp(packageName: String): Boolean {
        val pm = flowCtx.context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: run {
            LogRecorder.e("自动化", "$packageName 未安装")
            return false
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            flowCtx.context.startActivity(launchIntent)
            return true
        } catch (e: Exception) {
            LogRecorder.e("自动化", "重启应用失败: $packageName", e)
            return false
        }
    }

    protected fun getTodayString(): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return java.time.LocalDate.now().format(formatter)
    }

    protected suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
        return suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                if (task.isSuccessful && cont.isActive) cont.resume(task.result)
                else if (cont.isActive) cont.resumeWithException(task.exception ?: UnknownError())
            }
        }
    }
}