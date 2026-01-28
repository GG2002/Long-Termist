package com.cyc.yearlymemoir.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cyc.yearlymemoir.data.local.dao.BalanceDao
import com.cyc.yearlymemoir.data.local.dao.TransactionDao
import com.cyc.yearlymemoir.data.local.entity.BalanceEntity
import com.cyc.yearlymemoir.data.local.entity.TransactionEntity

@Database(
    entities = [
        BalanceEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {

    abstract fun balanceDao(): BalanceDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getInstance(context: Context): FinanceDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "yearlmemoir_finance.db"
                ).build()
                INSTANCE = instance
                instance
            }
    }
}
