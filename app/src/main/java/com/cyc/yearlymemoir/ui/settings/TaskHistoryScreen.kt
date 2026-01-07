package com.cyc.yearlymemoir.ui.settings

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyc.yearlymemoir.MainActivity
import com.cyc.yearlymemoir.data.AppDatabase
import com.cyc.yearlymemoir.data.TaskExecutionLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun TaskHistoryScreen(tag: String) {
    val context = MainActivity.appContext
    val scope = rememberCoroutineScope()

    var logs by remember { mutableStateOf<List<TaskExecutionLog>>(emptyList()) }
    var selected by remember { mutableStateOf<TaskExecutionLog?>(null) }

    LaunchedEffect(tag) {
        val dao = AppDatabase.get(context).taskExecutionLogDao()
        logs = withContext(Dispatchers.IO) { dao.getByTag(tag) }
    }
    val dateFormater = SimpleDateFormat(
        "MM-dd HH:mm", Locale.US
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "任务历史 ", style = typography.titleLarge)
                    Text(text = tag, style = typography.bodySmall)
                }
                IconButton(onClick = {
                    val dao = AppDatabase.get(context).taskExecutionLogDao()
                    // 清空当前标签的历史并刷新
                    scope.launch(Dispatchers.IO) {
                        dao.deleteByTag(tag)
                        val updated = dao.getByTag(tag)
                        withContext(Dispatchers.Main) { logs = updated }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "清空历史"
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (logs.isEmpty()) {
                Text("暂无历史记录", style = typography.bodyMedium)
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(logs) { log ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable { selected = log },
                            colors = CardDefaults.cardColors(
                                containerColor = colorScheme.surface
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = log.status,
                                        style = typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = (log.start_time ?: 0L).let {
                                        dateFormater.format(java.util.Date(it))
                                    }, style = typography.bodySmall)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = (log.output_result ?: "(无输出)").take(200),
                                    style = typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val detail = selected
    if (detail != null) {
        AlertDialog(
            onDismissRequest = { selected = null },
            confirmButton = {
                TextButton(onClick = { selected = null }) { Text("关闭") }
            },
            title = { Text("执行详情") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    @Composable
                    fun row(label: String, value: String?) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("$label:", fontWeight = FontWeight.Bold)
                            Text(value ?: "-")
                        }
                    }
                    row("ID", detail.id.toString())
                    row("Work UUID", detail.work_uuid)
                    row("Tag", detail.task_tag)
                    row("Status", detail.status)
                    row("Trigger", detail.trigger_type)
                    row("Input", detail.input_params)
                    row("Schedule", dateFormater.format(detail.schedule_time))
                    row("Start", dateFormater.format(detail.start_time))
                    row("End", dateFormater.format(detail.end_time))
                    Spacer(Modifier.height(8.dp))
                    Text("Output:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))

                    // 将输出按行展示为可滚动列表，便于查看长文本x
                    val outputLines = (detail.output_result ?: "(无)").lines()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(outputLines) { line ->
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                style = typography.bodySmall
                            )
                        }
                    }
                }
            }
        )
    }
}
