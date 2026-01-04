package com.cyc.yearlymemoir.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cyc.yearlymemoir.data.local.entity.BalanceEntity

@Dao
interface BalanceDao {

    @Upsert
    suspend fun upsert(balance: BalanceEntity)

    // 获取所有余额，不分余额来源
    @Query("SELECT * FROM balances ORDER BY record_date ASC")
    suspend fun getAllBalances(): List<BalanceEntity>

    // 获取某天的所有余额，不分余额来源
    @Query("SELECT * FROM balances WHERE record_date = :date")
    suspend fun getBalancesByDate(date: String): List<BalanceEntity>

    // 获取某余额来源的所有余额列表
    @Query("SELECT * FROM balances WHERE source_type = :source ORDER BY record_date ASC")
    suspend fun getBalancesBySource(source: String): List<BalanceEntity>
}
