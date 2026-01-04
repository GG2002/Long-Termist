package com.cyc.yearlymemoir.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cyc.yearlymemoir.data.local.entity.TransactionEntity

@Dao
interface TransactionDao {

    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    // 获取所有收入（amount > 0）
    @Query("SELECT * FROM transactions WHERE amount > 0 ORDER BY record_date ASC")
    suspend fun getAllIncomes(): List<TransactionEntity>

    // 获取所有支出（amount <= 0）
    @Query("SELECT * FROM transactions WHERE amount <= 0 ORDER BY record_date ASC")
    suspend fun getAllExpenses(): List<TransactionEntity>

    // 获取某天的所有收入（amount > 0）
    @Query("SELECT * FROM transactions WHERE record_date = :date AND amount > 0 ORDER BY record_date ASC")
    suspend fun getIncomesByDate(date: String): List<TransactionEntity>

    // 获取某天的所有支出（amount <= 0）
    @Query("SELECT * FROM transactions WHERE record_date = :date AND amount <= 0 ORDER BY record_date ASC")
    suspend fun getExpensesByDate(date: String): List<TransactionEntity>

    // 获取某 tag 的所有收入（amount > 0）
    @Query("SELECT * FROM transactions WHERE tag = :tag AND amount > 0 ORDER BY record_date ASC")
    suspend fun getIncomesByTag(tag: String): List<TransactionEntity>

    // 获取某 tag 的所有支出（amount <= 0）
    @Query("SELECT * FROM transactions WHERE tag = :tag AND amount <= 0 ORDER BY record_date ASC")
    suspend fun getExpensesByTag(tag: String): List<TransactionEntity>

    // 获取收入的所有 tag，去重
    @Query("SELECT DISTINCT tag FROM transactions WHERE amount > 0")
    suspend fun getIncomeTags(): List<String>

    // 获取支出的所有 tag，去重
    @Query("SELECT DISTINCT tag FROM transactions WHERE amount <= 0")
    suspend fun getExpenseTags(): List<String>
}
