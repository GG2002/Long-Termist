package com.cyc.yearlymemoir.domain.repository

import com.cyc.yearlymemoir.domain.model.Detail
import com.cyc.yearlymemoir.domain.model.YearlyDetail
import com.cyc.yearlymemoir.domain.model.Field
import com.cyc.yearlymemoir.domain.model.Group
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.model.BalanceRecord
import com.cyc.yearlymemoir.domain.model.TransactionRecord

enum class TimePeriod {
    DAY,
    MONTH,
    YEAR
}

interface YearlyMemoirRepository {

    suspend fun getAllDetails(): List<Detail>

    suspend fun getDetailByFieldAndUniversalDate(
        fieldId: Int,
        universalDate: UniversalDate
    ): Detail?

    // Yearly details APIs
    suspend fun getAllYearlyDetails(): List<YearlyDetail>
    suspend fun getYearlyDetailByFieldAndMdDate(fieldId: Int, mdDate: String): YearlyDetail?
    suspend fun upsertYearlyDetail(detail: YearlyDetail)
    suspend fun deleteYearlyDetail(detail: YearlyDetail)

    suspend fun getFirstDayFieldDataByName(
        name: String,
        period: TimePeriod,
        isAsc: Boolean = true
    ): Pair<List<Detail>, String> // 返回值可以封装成一个更明确的 Result 类

    suspend fun insertOrUpdateDetail(
        fieldName: String,
        detail: String,
        date: UniversalDate
    )

    suspend fun deleteDetail(detail: Detail)

    suspend fun insertOrUpdateField(field: Field)

    suspend fun getAllFields(): List<Field>

    suspend fun getFieldByName(name: String): Field?

    suspend fun getAllGroupsWithFields(): Map<Group, List<Field>>

    // Balance APIs
    suspend fun upsertBalance(balance: BalanceRecord)
    suspend fun getAllBalances(): List<BalanceRecord>
    suspend fun getBalancesByDate(date: String): List<BalanceRecord>
    suspend fun getBalancesBySource(sourceType: String): List<BalanceRecord>

    // Transaction APIs (split income/expense)
    suspend fun upsertTransaction(record: TransactionRecord)
    suspend fun getAllIncomes(): List<TransactionRecord>
    suspend fun getAllExpenses(): List<TransactionRecord>
    suspend fun getIncomesByDate(date: String): List<TransactionRecord>
    suspend fun getExpensesByDate(date: String): List<TransactionRecord>
    suspend fun getIncomesByTag(tag: String): List<TransactionRecord>
    suspend fun getExpensesByTag(tag: String): List<TransactionRecord>
    suspend fun getIncomeTags(): List<String>
    suspend fun getExpenseTags(): List<String>
}