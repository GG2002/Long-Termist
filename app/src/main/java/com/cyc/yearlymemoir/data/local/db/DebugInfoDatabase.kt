package com.cyc.yearlymemoir.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cyc.yearlymemoir.data.local.dao.TaskExecutionLogDao
import com.cyc.yearlymemoir.data.local.entity.TaskExecutionLog

@Database(entities = [TaskExecutionLog::class], version = 1, exportSchema = false)
abstract class DebugInfoDatabase : RoomDatabase() {
    abstract fun taskExecutionLogDao(): TaskExecutionLogDao

    companion object {
        @Volatile
        private var INSTANCE: DebugInfoDatabase? = null

        fun get(context: Context): DebugInfoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DebugInfoDatabase::class.java,
                    "yearlymemoir_blackbox.db"
                ).fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
