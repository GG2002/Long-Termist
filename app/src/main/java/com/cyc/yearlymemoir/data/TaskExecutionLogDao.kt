package com.cyc.yearlymemoir.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TaskExecutionLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: TaskExecutionLog): Long

    @Query("UPDATE task_execution_log SET status = :status, output_result = :output, end_time = :endTime WHERE id = :id")
    suspend fun updateResult(id: Long, status: String, output: String?, endTime: Long)

    @Query("SELECT * FROM task_execution_log ORDER BY start_time DESC")
    suspend fun getAll(): List<TaskExecutionLog>

    @Query("SELECT * FROM task_execution_log WHERE task_tag LIKE '%' || :tag ORDER BY start_time DESC")
    suspend fun getByTag(tag: String): List<TaskExecutionLog>

    @Query("DELETE FROM task_execution_log WHERE task_tag LIKE '%' || :tag")
    suspend fun deleteByTag(tag: String)
}
