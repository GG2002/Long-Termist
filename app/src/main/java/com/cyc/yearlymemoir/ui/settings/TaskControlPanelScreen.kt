package com.cyc.yearlymemoir.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.WorkScheduler

/**
 * 任务控制台：展示已注册任务、当前状态，并提供操作入口。
 * 注：这里使用 WorkScheduler 的注册表和统一调度/取消接口。
 */
@Composable
fun TaskControlPanelScreen() {
    val context = MainActivity.appContext

    // 使用 WorkScheduler 的注册表
    val tasks = WorkScheduler.registry.map { TaskMeta(it.tag, it.name, it.description) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
    Column(
            modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "任务控制台", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            tasks.forEach { task ->
                val statusText = remember { mutableStateOf("查询中...") }

                LaunchedEffect(task.tag) {
                    // 观察/查询 WorkManager 中该 tag 的状态（简化：拉取一次）
                    val infos = WorkManager.getInstance(context).getWorkInfosByTag(task.tag).get()
                    val current = infos.firstOrNull()?.state
                    statusText.value = when (current) {
                        WorkInfo.State.ENQUEUED -> "排队中"
                        WorkInfo.State.RUNNING -> "运行中"
                        WorkInfo.State.SUCCEEDED -> "已成功"
                        WorkInfo.State.FAILED -> "失败"
                        WorkInfo.State.BLOCKED -> "被阻塞"
                        WorkInfo.State.CANCELLED -> "已取消"
                        else -> "未知"
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { MainActivity.navController.navigate("TaskHistory/${task.tag}") },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(task.name, style = MaterialTheme.typography.titleMedium)
                        Text(task.description, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "当前状态：${statusText.value}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(onClick = {
                                WorkScheduler.runTaskNow(
                                    context,
                                    task.tag
                                )
                            }) { Text("立即运行") }
                            Button(onClick = {
                                WorkScheduler.cancelByTag(
                                    context,
                                    task.tag
                                )
                            }) { Text("取消所有") }
                            Button(onClick = { /* TODO: 弹窗配置约束 */ }) { Text("配置约束") }
                        }
                    }
                }
            }
        }
    }
}

data class TaskMeta(
    val tag: String,
    val name: String,
    val description: String
)

// 统一操作由 WorkScheduler 提供
