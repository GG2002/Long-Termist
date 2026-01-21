package com.cyc.yearlymemoir.venassists

import android.content.Context
import com.cyc.yearlymemoir.MainApplication
import kotlinx.coroutines.CompletableDeferred

/**
 * 用于在各个 Flow 之间共享状态和工具
 */
class FlowContext {
    val context: Context = MainApplication.instance
    val ds = MainApplication.ds
    val model = MainApplication.repository

    // 全局共享的 deferred，用于最后通知结束
    var deferred: CompletableDeferred<String>? = null

    // 尝试从 op.data 中提取 deferred
    // 只有当前步骤是整个流程的第一步时，data 才不为 null
    fun tryUpdateDeferred(data: Any?) {
        if (data is CompletableDeferred<*>) {
            @Suppress("UNCHECKED_CAST")
            deferred = data as CompletableDeferred<String>
        }
    }
}