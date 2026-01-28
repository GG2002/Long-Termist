package com.cyc.yearlymemoir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_execution_log")
data class TaskExecutionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val work_uuid: String,
    val task_tag: String,
    val status: String, // STARTING, SUCCESS, FAILURE, RETRY, STOPPED
    val input_params: String?,
    val output_result: String?,
    val trigger_type: String?, // AUTO, MANUAL
    val schedule_time: Long?,
    val start_time: Long?,
    val end_time: Long?
)
