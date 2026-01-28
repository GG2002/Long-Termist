package com.cyc.yearlymemoir.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cyc.yearlymemoir.data.local.dao.FinanceAssetDao
import com.cyc.yearlymemoir.data.local.entity.FinanceAsset

@Database(
    entities = [FinanceAsset::class], version = 1, exportSchema = false
)
abstract class TmpFinanceDataBase : RoomDatabase() {
    abstract fun financeAssetDao(): FinanceAssetDao

    companion object {
        @Volatile
        private var INSTANCE: TmpFinanceDataBase? = null

        fun get(context: Context): TmpFinanceDataBase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, TmpFinanceDataBase::class.java, "tmp_finance.db"
            ).fallbackToDestructiveMigration(false).build().also { INSTANCE = it }
        }
    }
}