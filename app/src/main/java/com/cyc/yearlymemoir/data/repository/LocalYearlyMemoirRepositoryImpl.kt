package com.cyc.yearlymemoir.data.repository

import com.cyc.yearlymemoir.data.local.dao.BalanceDao
import com.cyc.yearlymemoir.data.local.dao.DetailDao
import com.cyc.yearlymemoir.data.local.dao.FieldDao
import com.cyc.yearlymemoir.data.local.dao.TransactionDao
import com.cyc.yearlymemoir.data.local.dao.YearlyDetailDao
import com.cyc.yearlymemoir.data.local.entity.toDomainModel
import com.cyc.yearlymemoir.data.local.entity.toEntity
import com.cyc.yearlymemoir.domain.model.BalanceRecord
import com.cyc.yearlymemoir.domain.model.Detail
import com.cyc.yearlymemoir.domain.model.Field
import com.cyc.yearlymemoir.domain.model.Group
import com.cyc.yearlymemoir.domain.model.TransactionRecord
import com.cyc.yearlymemoir.domain.model.UniversalDate
import com.cyc.yearlymemoir.domain.model.YearlyDetail
import com.cyc.yearlymemoir.domain.repository.TimePeriod
import com.cyc.yearlymemoir.domain.repository.YearlyMemoirRepository

class LocalYearlyMemoirRepository(
    private val detailDao: DetailDao,
    private val yearlyDetailDao: YearlyDetailDao,
    private val fieldDao: FieldDao,
    private val balanceDao: BalanceDao,
    private val transactionDao: TransactionDao,
) : YearlyMemoirRepository {
    override suspend fun getAllDetails(): List<Detail> {
        return detailDao.getAllDetails().map { it.toDomainModel() }
    }

    override suspend fun getDetailByFieldAndUniversalDate(
        fieldId: Int,
        universalDate: UniversalDate
    ): Detail? {
        val year = universalDate.getRawYear()
        val mdDate = universalDate.getRawMDDate()

        return detailDao.getDetailByFieldAndDate(
            fieldId, year, mdDate.toString()
        )?.toDomainModel()
    }

    override suspend fun getFirstDayFieldDataByName(
        name: String,
        period: TimePeriod,
        isAsc: Boolean
    ): Pair<List<Detail>, String> {
        val field = fieldDao.getFieldByName(name)?.toDomainModel()
            ?: throw Exception("Field not found: $name")

        val detailsEntity = when (period) {
            TimePeriod.DAY -> detailDao.getDayFieldDataById(field.fieldId)

            TimePeriod.MONTH -> detailDao.getFirstDayOfMonthDataById(field.fieldId)
            TimePeriod.YEAR -> detailDao.getFirstDayOfYearDataById(field.fieldId)
        }

        val detailsDomain = detailsEntity.map { it.toDomainModel() }

        // Room 不直接支持 DESC/ASC 参数，你可以在查询后手动反转列表
        val sortedDetails = if (isAsc) detailsDomain else detailsDomain.reversed()

        return Pair(sortedDetails, field.fieldType)
    }

    override suspend fun insertOrUpdateDetail(
        fieldName: String,
        detail: String,
        date: UniversalDate
    ) {
        val fieldId = fieldDao.getFieldByName(fieldName)!!.fieldId
        val detailEntity = Detail(
            year = date.getRawYear(),
            mdDate = date,
            fieldId = fieldId,
            detail = detail,
        ).toEntity()
        detailDao.upsertDetail(detailEntity)
    }

    override suspend fun deleteDetail(detail: Detail) {
        detailDao.delete(detail.toEntity())
    }

    override suspend fun insertOrUpdateField(field: Field) {
        fieldDao.insertField(field.toEntity())
    }

    override suspend fun getAllFields(): List<Field> {
        return fieldDao.getAllFields().map { it.toDomainModel() }
    }


    override suspend fun getFieldByName(name: String): Field? {
        return fieldDao.getFieldByName(name)?.toDomainModel()
    }

    override suspend fun getAllGroupsWithFields(): Map<Group, List<Field>> {
        TODO("Not yet implemented")
    }

    // Yearly details
    override suspend fun getAllYearlyDetails(): List<YearlyDetail> {
        return yearlyDetailDao.getAll().map { it.toDomainModel() }
    }

    override suspend fun getYearlyDetailByFieldAndMdDate(
        fieldId: Int,
        mdDate: String
    ): YearlyDetail? {
        return yearlyDetailDao.getByFieldAndMdDate(fieldId, mdDate)?.toDomainModel()
    }

    override suspend fun upsertYearlyDetail(detail: YearlyDetail) {
        yearlyDetailDao.upsert(detail.toEntity())
    }

    override suspend fun deleteYearlyDetail(detail: YearlyDetail) {
        yearlyDetailDao.delete(detail.toEntity())
    }

    // Balance APIs
    override suspend fun upsertBalance(balance: BalanceRecord) {
        balanceDao.upsert(balance.toEntity())
    }

    override suspend fun getAllBalances(): List<BalanceRecord> {
        return balanceDao.getAllBalances().map { it.toDomainModel() }
    }

    override suspend fun getBalancesByDate(date: String): List<BalanceRecord> {
        return balanceDao.getBalancesByDate(date).map { it.toDomainModel() }
    }

    override suspend fun getBalancesBySource(sourceType: String): List<BalanceRecord> {
        return balanceDao.getBalancesBySource(sourceType).map { it.toDomainModel() }
    }

    // Transaction APIs
    override suspend fun upsertTransaction(record: TransactionRecord) {
        transactionDao.upsert(record.toEntity())
    }

    override suspend fun deleteTransaction(id: Int) {
        transactionDao.deleteById(id)
    }

    override suspend fun getAllIncomes(): List<TransactionRecord> {
        return transactionDao.getAllIncomes().map { it.toDomainModel() }
    }

    override suspend fun getAllExpenses(): List<TransactionRecord> {
        return transactionDao.getAllExpenses().map { it.toDomainModel() }
    }

    override suspend fun getIncomesByDate(date: String): List<TransactionRecord> {
        return transactionDao.getIncomesByDate(date).map { it.toDomainModel() }
    }

    override suspend fun getExpensesByDate(date: String): List<TransactionRecord> {
        return transactionDao.getExpensesByDate(date).map { it.toDomainModel() }
    }

    override suspend fun getIncomesByTag(tag: String): List<TransactionRecord> {
        return transactionDao.getIncomesByTag(tag).map { it.toDomainModel() }
    }

    override suspend fun getExpensesByTag(tag: String): List<TransactionRecord> {
        return transactionDao.getExpensesByTag(tag).map { it.toDomainModel() }
    }

    override suspend fun getIncomeTags(): List<String> {
        return transactionDao.getIncomeTags()
    }

    override suspend fun getExpenseTags(): List<String> {
        return transactionDao.getExpenseTags()
    }

    override suspend fun getAllTransactionsDesc(): List<TransactionRecord> {
        return transactionDao.getAllTransactionsDesc().map { it.toDomainModel() }
    }
}